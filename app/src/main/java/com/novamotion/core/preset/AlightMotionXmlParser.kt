package com.novamotion.core.preset

import com.novamotion.core.model.*
import java.io.StringWriter
import javax.xml.parsers.DocumentBuilderFactory
import javax.xml.transform.OutputKeys
import javax.xml.transform.TransformerFactory
import javax.xml.transform.dom.DOMSource
import javax.xml.transform.stream.StreamResult

object AlightMotionXmlParser {

    /**
     * Exports a Project into Alight Motion compatible XML format.
     */
    fun exportToAlightMotionXml(project: Project): String {
        val docBuilder = DocumentBuilderFactory.newInstance().newDocumentBuilder()
        val doc = docBuilder.newDocument()

        val rootElement = doc.createElement("scene").apply {
            setAttribute("width", project.width.toString())
            setAttribute("height", project.height.toString())
            setAttribute("fps", project.fps.toString())
            setAttribute("duration", (project.durationMs / 1000f).toString())
        }
        doc.appendChild(rootElement)

        for (layer in project.layers) {
            val shapeElement = doc.createElement("shape").apply {
                setAttribute("type", if (layer.type == LayerType.TEXT) "text" else "rect")
                setAttribute("startTime", (layer.startTimeMs / 1000f).toString())
                setAttribute("endTime", (layer.endTimeMs / 1000f).toString())
            }

            val transformElement = doc.createElement("transform")
            val propertyElement = doc.createElement("property").apply {
                setAttribute("name", "position")
            }

            for (k in layer.transform.posX.keyframes) {
                val kfElement = doc.createElement("keyframe").apply {
                    setAttribute("time", (k.timeMs / 1000f).toString())
                    setAttribute("value", "${k.value}, 0.0")
                    setAttribute("curve", "${k.curve.x1}, ${k.curve.y1}, ${k.curve.x2}, ${k.curve.y2}")
                }
                propertyElement.appendChild(kfElement)
            }

            transformElement.appendChild(propertyElement)
            shapeElement.appendChild(transformElement)
            rootElement.appendChild(shapeElement)
        }

        val transformer = TransformerFactory.newInstance().newTransformer().apply {
            setOutputProperty(OutputKeys.INDENT, "yes")
            setOutputProperty("{http://xml.apache.org/xslt}indent-amount", "2")
        }

        val writer = StringWriter()
        transformer.transform(DOMSource(doc), StreamResult(writer))
        return writer.toString()
    }
}
