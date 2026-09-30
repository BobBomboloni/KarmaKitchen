with open("gradle/libs.versions.toml", "r") as f:
    content = f.read()

content = content.replace('[libraries]', 'mapsCompose = "4.3.3"\nplayServicesMaps = "18.2.0"\n\n[libraries]\nmaps-compose = { group = "com.google.maps.android", name = "maps-compose", version.ref = "mapsCompose" }\nplay-services-maps = { group = "com.google.android.gms", name = "play-services-maps", version.ref = "playServicesMaps" }\n')

content += 'google-services = { id = "com.google.gms.google-services", version.ref = "googleServices" }\n'

with open("gradle/libs.versions.toml", "w") as f:
    f.write(content)
