package org.wpfy.carlauncher.feature.trip

import java.io.Writer
import java.math.BigDecimal
import java.time.Instant

object TripGpx {
    /** Writes to caller-owned storage; no permissions, filesystem paths or sharing policy here. */
    fun write(details: TripDetails, writer: Writer) {
        writer.write("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n")
        writer.write("<gpx version=\"1.1\" creator=\"Car Launcher\" xmlns=\"http://www.topografix.com/GPX/1/1\"><trk><name>")
        writer.write(escape(details.trip.startAddress ?: "Trip ${Instant.ofEpochMilli(details.trip.startedAt)}"))
        writer.write("</name>")
        var segment: Int? = null
        details.points.forEach { point ->
            if (segment != point.segment) {
                if (segment != null) writer.write("</trkseg>")
                writer.write("<trkseg>")
                segment = point.segment
            }
            writer.write("<trkpt lat=\"${decimal(point.latitude)}\" lon=\"${decimal(point.longitude)}\">")
            point.altitudeM?.let { writer.write("<ele>${decimal(it)}</ele>") }
            writer.write("<time>${Instant.ofEpochMilli(point.timestampMillis)}</time></trkpt>")
        }
        if (segment != null) writer.write("</trkseg>")
        writer.write("</trk></gpx>\n")
    }

    /** GPX `lat`, `lon` and `ele` are `xsd:decimal`, which has no exponent form. */
    private fun decimal(value: Double): String = BigDecimal.valueOf(value).toPlainString()

    private fun escape(value: String) = buildString {
        value.forEach { char ->
            append(when (char) {
                '&' -> "&amp;"
                '<' -> "&lt;"
                '>' -> "&gt;"
                '"' -> "&quot;"
                '\'' -> "&apos;"
                else -> if (char == '\t' || char == '\n' || char == '\r' || char >= ' ') char.toString() else ""
            })
        }
    }
}
