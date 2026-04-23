package com.campusride.app;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.android.gms.maps.model.LatLng;

public final class WebMapHtmlBuilder {

    private WebMapHtmlBuilder() {
    }

    @NonNull
    public static String buildSingleLocationMap(@Nullable LatLng latLng,
                                                @NonNull String title,
                                                @NonNull String subtitle) {
        LatLng center = latLng != null ? latLng : new LatLng(20.5937, 78.9629);
        boolean hasMarker = latLng != null;
        int zoom = hasMarker ? 14 : 5;

        return "<!DOCTYPE html><html><head>"
                + "<meta name='viewport' content='width=device-width, initial-scale=1.0'>"
                + "<link rel='stylesheet' href='https://unpkg.com/leaflet@1.9.4/dist/leaflet.css'/>"
                + "<script src='https://unpkg.com/leaflet@1.9.4/dist/leaflet.js'></script>"
                + "<style>html,body,#map{height:100%;margin:0;}body{background:#101614;}</style>"
                + "</head><body><div id='map'></div><script>"
                + "var map=L.map('map',{zoomControl:true}).setView([" + center.latitude + "," + center.longitude + "]," + zoom + ");"
                + "L.tileLayer('https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png',{maxZoom:19,attribution:'&copy; OpenStreetMap'}).addTo(map);"
                + (hasMarker
                ? "L.marker([" + center.latitude + "," + center.longitude + "]).addTo(map).bindPopup('"
                + escape(title) + "<br/>" + escape(subtitle) + "').openPopup();"
                : "")
                + "</script></body></html>";
    }

    @NonNull
    public static String buildRouteMap(@NonNull LatLng origin,
                                       @NonNull String originTitle,
                                       @NonNull LatLng destination,
                                       @NonNull String destinationTitle) {
        return "<!DOCTYPE html><html><head>"
                + "<meta name='viewport' content='width=device-width, initial-scale=1.0'>"
                + "<link rel='stylesheet' href='https://unpkg.com/leaflet@1.9.4/dist/leaflet.css'/>"
                + "<script src='https://unpkg.com/leaflet@1.9.4/dist/leaflet.js'></script>"
                + "<style>html,body,#map{height:100%;margin:0;}body{background:#101614;}</style>"
                + "</head><body><div id='map'></div><script>"
                + "var map=L.map('map',{zoomControl:true});"
                + "L.tileLayer('https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png',{maxZoom:19,attribution:'&copy; OpenStreetMap'}).addTo(map);"
                + "var origin=[" + origin.latitude + "," + origin.longitude + "];"
                + "var destination=[" + destination.latitude + "," + destination.longitude + "];"
                + "L.marker(origin).addTo(map).bindPopup('" + escape(originTitle) + "');"
                + "L.marker(destination).addTo(map).bindPopup('" + escape(destinationTitle) + "');"
                + "var line=L.polyline([origin,destination],{color:'#0F8B6D',weight:5}).addTo(map);"
                + "map.fitBounds(line.getBounds(),{padding:[30,30]});"
                + "</script></body></html>";
    }

    @NonNull
    private static String escape(@NonNull String value) {
        return value.replace("'", "\\'").replace("\"", "&quot;");
    }
}
