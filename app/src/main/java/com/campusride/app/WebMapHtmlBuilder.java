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
        String osrmEndpoint = "https://router.project-osrm.org/route/v1/driving/"
                + origin.longitude + "," + origin.latitude + ";"
                + destination.longitude + "," + destination.latitude
                + "?overview=full&geometries=geojson";

        return "<!DOCTYPE html><html><head>"
                + "<meta name='viewport' content='width=device-width, initial-scale=1.0'>"
                + "<link rel='stylesheet' href='https://unpkg.com/leaflet@1.9.4/dist/leaflet.css'/>"
                + "<script src='https://unpkg.com/leaflet@1.9.4/dist/leaflet.js'></script>"
                + "<style>html,body,#map{height:100%;margin:0;}body{background:#101614;}"
                + ".route-status{position:absolute;left:12px;top:12px;z-index:500;background:#101614;"
                + "color:#f2f7f5;border:1px solid #2bc29b;border-radius:8px;padding:8px 10px;"
                + "font:13px sans-serif;box-shadow:0 4px 12px rgba(0,0,0,.2);}</style>"
                + "</head><body><div id='map'></div><script>"
                + "var map=L.map('map',{zoomControl:true});"
                + "L.tileLayer('https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png',{maxZoom:19,attribution:'&copy; OpenStreetMap'}).addTo(map);"
                + "var origin=[" + origin.latitude + "," + origin.longitude + "];"
                + "var destination=[" + destination.latitude + "," + destination.longitude + "];"
                + "L.marker(origin).addTo(map).bindPopup('" + escape(originTitle) + "');"
                + "L.marker(destination).addTo(map).bindPopup('" + escape(destinationTitle) + "');"
                + "var status=document.createElement('div');status.className='route-status';status.textContent='Loading road route...';document.body.appendChild(status);"
                + "function fit(layer){map.fitBounds(layer.getBounds(),{padding:[36,36]});}"
                + "function fallback(){status.textContent='Road route unavailable';var line=L.polyline([origin,destination],{color:'#FBBF24',weight:5,dashArray:'8 8'}).addTo(map);fit(line);}"
                + "fetch('" + osrmEndpoint + "').then(function(response){return response.json();}).then(function(data){"
                + "if(!data.routes||!data.routes.length||!data.routes[0].geometry){fallback();return;}"
                + "var coordinates=data.routes[0].geometry.coordinates.map(function(point){return [point[1],point[0]];});"
                + "var route=L.polyline(coordinates,{color:'#0F8B6D',weight:6,opacity:.95}).addTo(map);"
                + "var distanceKm=(data.routes[0].distance/1000).toFixed(1);"
                + "var minutes=Math.max(1,Math.round(data.routes[0].duration/60));"
                + "status.textContent=distanceKm+' km route - '+minutes+' min drive';fit(route);"
                + "}).catch(fallback);"
                + "</script></body></html>";
    }

    @NonNull
    private static String escape(@NonNull String value) {
        return value.replace("'", "\\'").replace("\"", "&quot;");
    }
}
