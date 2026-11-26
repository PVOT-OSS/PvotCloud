// SPDX-FileCopyrightText: 2026 Saalim Quadri <danascape@gmail.com>
// SPDX-License-Identifier: AGPL-3.0-or-later

package org.prauga.cloud.repo.tree

import java.time.OffsetDateTime
import java.time.format.DateTimeParseException
import kotlinx.serialization.SerializationException
import org.prauga.cloud.repo.Id
import org.prauga.cloud.repo.ResticJson

class Tree(nodes: List<Node>) {

    val nodes: List<Node> = nodes.sortedWith(NAME_ORDER)

    init {
        this.nodes.zipWithNext().forEach { (a, b) ->
            require(NAME_ORDER.compare(a, b) != 0) { "duplicate name ${a.name}" }
        }
    }

    fun encode(): ByteArray {
        val out = StringBuilder("{\"nodes\":[")
        nodes.forEachIndexed { index, node ->
            if (index > 0) out.append(',')
            appendNode(out, node)
        }
        return out.append("]}\n").toString().toByteArray(Charsets.UTF_8)
    }

    companion object {
        private val NAME_ORDER = Comparator<Node> { a, b -> compareUtf8(a.name, b.name) }

        fun decode(data: ByteArray): Tree {
            val json =
                try {
                    ResticJson.decodeFromString(NodeJson.Tree.serializer(), data.decodeToString())
                } catch (e: SerializationException) {
                    throw IllegalArgumentException("invalid tree", e)
                }
            return Tree(json.nodes.map(::fromJson))
        }

        private fun fromJson(json: NodeJson): Node {
            val type =
                NodeType.entries.firstOrNull { it.json == json.type }
                    ?: throw IllegalArgumentException("unknown node type ${json.type}")
            return Node(
                name = GoStrings.unquote(json.name),
                type = type,
                modTime = parseTime(json.mtime),
                accessTime = parseTime(json.atime),
                changeTime = parseTime(json.ctime),
                mode = json.mode,
                uid = json.uid,
                gid = json.gid,
                user = json.user,
                group = json.group,
                inode = json.inode,
                deviceId = json.deviceId,
                size = json.size,
                links = json.links,
                linkTarget = json.linktarget,
                device = json.device,
                content = json.content?.map(::Id),
                subtree = json.subtree?.let(::Id),
            )
        }

        private fun parseTime(value: String): OffsetDateTime =
            try {
                OffsetDateTime.parse(value)
            } catch (e: DateTimeParseException) {
                throw IllegalArgumentException("invalid time $value", e)
            }

        private fun appendNode(out: StringBuilder, node: Node) {
            out.append("{\"name\":").append(GoStrings.jsonString(GoStrings.quote(node.name)))
            out.append(",\"type\":").append(GoStrings.jsonString(node.type.json))
            if (node.mode != 0L) out.append(",\"mode\":").append(node.mode)
            out.append(",\"mtime\":").append(GoStrings.jsonString(GoStrings.time(node.modTime)))
            out.append(",\"atime\":").append(GoStrings.jsonString(GoStrings.time(node.accessTime)))
            out.append(",\"ctime\":").append(GoStrings.jsonString(GoStrings.time(node.changeTime)))
            out.append(",\"uid\":").append(node.uid)
            out.append(",\"gid\":").append(node.gid)
            node.user
                ?.takeIf { it.isNotEmpty() }
                ?.let { out.append(",\"user\":").append(GoStrings.jsonString(it)) }
            node.group
                ?.takeIf { it.isNotEmpty() }
                ?.let { out.append(",\"group\":").append(GoStrings.jsonString(it)) }
            if (node.inode != 0L) out.append(",\"inode\":").append(node.inode)
            if (node.deviceId != 0L) out.append(",\"device_id\":").append(node.deviceId)
            if (node.size != 0L) out.append(",\"size\":").append(node.size)
            if (node.links != 0L) out.append(",\"links\":").append(node.links)
            node.linkTarget
                ?.takeIf { it.isNotEmpty() }
                ?.let { out.append(",\"linktarget\":").append(GoStrings.jsonString(it)) }
            if (node.device != 0L) out.append(",\"device\":").append(node.device)
            out.append(",\"content\":")
            val content = node.content
            if (content == null) out.append("null")
            else
                content.joinTo(out, separator = ",", prefix = "[", postfix = "]") {
                    "\"${it.hex}\""
                }
            node.subtree?.let { out.append(",\"subtree\":\"").append(it.hex).append('"') }
            out.append('}')
        }

        private fun compareUtf8(a: String, b: String): Int {
            val x = a.toByteArray(Charsets.UTF_8)
            val y = b.toByteArray(Charsets.UTF_8)
            for (i in 0 until minOf(x.size, y.size)) {
                val diff = (x[i].toInt() and 0xff) - (y[i].toInt() and 0xff)
                if (diff != 0) return diff
            }
            return x.size - y.size
        }
    }
}
