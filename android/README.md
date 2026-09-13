# Auto-Polish Android 1.3.21

## Installation

APK auf das Handy übertragen und als Update installieren. Android 10 oder neuer. Paketkennung de.autopolish.nativeapp. Die Signatur entspricht der bisherigen lokalen APK.

## Änderungen

- Glocke ohne weißen Hintergrund: bei vorhandenen Benachrichtigungen langsames Blinken im 2,4-Sekunden-Takt. Bei abgeschalteten Android-Animationen bleibt sie statisch.
- Weißes transparentes Kopf-Logo, blau-schwarzes App-Symbol, Abstände zu Android-Systemleisten.
- Native Formulare für Angebote und Kundenlinks, Arbeitskarten, Radeinlagerung, geschützte Notizen und Briefpapier-Einstellungen an die bestehenden Server-Schnittstellen angeschlossen. Die bisherigen Platzhalter wurden ersetzt.
- Aufträge mit Positionen, Rabatt, Fotos, Fahrzeugschein und Unterschrift erstellen und bearbeiten. Vorhandene Medien bleiben beim Bearbeiten erhalten.
- Fotos über Kamera oder Galerie, native Unterschrift, PDF-Anzeige, Speichern, Drucken und Teilen. Teilen und E-Mail öffnen die passende Anwendung zur weiteren Bedienung.
- Globale Suche und direkte Bearbeitung aus den Listen. Arbeitskarten aus Aufträgen erstellen.
- Profil, Zahnrad-Einstellungen, lokaler App-Dark-Mode und speicherbare Dashboard-Anordnung aus Version 1.3.16 enthalten.

## Grenzen und offene Einrichtung

Push bei geschlossener App ist noch nicht eingerichtet. Dafür fehlen Firebase-Projekt und die Anbindung des Servers an den Pushdienst. Die Glocke lädt während der Anzeige des Dashboards alle 30 Sekunden neue Daten; das ist kein Hintergrund-Push.

Der Kalender öffnet die auf dem Handy installierte Kalender-App für den ausgewählten Tag und kann dort einen Termin vorbereiten. Die eingebettete Google-Kalenderansicht der Website ist damit nicht vollständig in der App nachgebildet. Dafür muss eine passende Kalender-App vorhanden und das gewünschte Konto dort verbunden sein.

Server-Funktionen benötigen eine Internetverbindung sowie passende Benutzerrechte. Es wurden keine Änderungen am laufenden Webserver veröffentlicht.

## Prüfung

21 Java-Dateien erfolgreich kompiliert. 28 isolierte Prüfungen für Preisberechnung, Medien-Datenübernahme und HTTP-Übertragung bestanden. Diese Prüfungen verwenden simulierte Serverantworten und verändern keine Live-Daten. Android-Ressourcen und APK-Paketierung erfolgreich; Signatur und Versionsdaten separat geprüft.

Kein Bedienungs- oder Darstellungstest auf einem Android-Handy durchgeführt. Insbesondere Speichern und erneutes Öffnen echter Datensätze, Kamera, Freigaben, Druck und Darstellung müssen noch auf einem Gerät geprüft werden. Die Implementierung ist daher nicht als vollständig im Alltag getestet zu verstehen.

In der lokalen Prüfumgebung trat beim Schließen von Java-Archiven ein Windows-Zugriffsfehler auf. Die Java-Kompilierung wurde erfolgreich separat über die Compiler-API durchgeführt, anschließend wurden diese Klassen regulär paketiert. Das Quellprojekt enthält keine übersprungene Kompilierung.

## Android Studio

ZIP in einen neuen Ordner entpacken und öffnen. Java 17, Gradle 8.9, Android Gradle Plugin 8.7.3, Compile/Target SDK 35, Min SDK 29. APK über das Build-Menü erzeugen. Für weitere Updates mit identischer Signatur den bisherigen Signierschlüssel verwenden; dieser ist nicht im ZIP enthalten.


## Änderungen in 1.3.18

Dashboard-Titel auf 19 sp verkleinert. Bearbeiten-Schaltfläche klein am unteren rechten Ende des Dashboards. Native-Zusatz aus Versionsanzeige entfernt. Einheitliche 48/52-dp-Aktionsflächen, getrennte Abstände, dezente sekundäre Aktionen ohne Schatten. Logos in Auftragsliste und Anmeldung transparent und passend zum Hintergrund.

Neue Einlagerungen verwenden RAD- und vier Ziffern; die App prüft den Vorschlag auf vorhandene Nummern. Bestehende Nummern bleiben erhalten. Gleichzeitige Nummernvergabe auf mehreren Geräten benötigt weiterhin eine atomare Prüfung auf dem Server.

Gespeicherte Radeinlagerungen bieten oben einen Formular-Vorschauknopf. Die App lädt das Original-PDF; bei einem Serverfehler oder einer Zeitüberschreitung wird eine paginierte Ersatz-PDF mit gespeicherten Kundendaten, Reifendaten, Preis, Hinweisen, Unterschrift und Fotos erstellt. Diese Ersatzdarstellung entspricht nicht pixelgenau dem Webformular und enthält keinen Barcode. Bei fehlgeschlagenem Laden werden Druck- und Speicherknöpfe ausgeblendet und ein erneuter Versuch angeboten.

80 ausdrücklich angeforderte Testdatensätze auf auftraege.auto-polish.de angelegt: jeweils 20 Aufträge, Angebote, Arbeitskarten und Radeinlagerungen. Alle 80 durch separate GET-Abfragen wieder geöffnet. Fünf Testeinlagerungen vom 01.02.2026 erzeugen fünf zusätzliche, nicht ausgeblendete Glockenmeldungen. Erfundenen Namen wurde TEST hinzugefügt; keine Kunden wurden angeschrieben.

22 Java-Dateien kompiliert. Kein Android-Gerätetest durchgeführt; insbesondere die Ersatz-PDF benötigt noch eine Prüfung auf dem Handy.

Der Serverfehler wurde an einem bestehenden Datensatz reproduziert: WinAnsi cannot encode newline (0x000a). Die App-Ersatzdarstellung umgeht diesen Fehler; der Server selbst wurde nicht aktualisiert.


## Änderungen in 1.3.19

- Kleines Barcode-Symbol innerhalb der Suchfelder: Dashboard, Auftragsliste und Listen/Suche der Fachmodule. Ergebnis wird eingetragen und die Suche gestartet. Scanner nutzt die Rückkamera mit Kamerafreigabe; Bilddaten werden lokal verarbeitet und nicht gespeichert oder hochgeladen. Bei verweigerter Freigabe bleibt die Texteingabe nutzbar.
- Code 128, Code 39, EAN und QR unterstützt. Sieben isolierte Tests mit echten Code-128-Bildern: kurze/lange Nummern, gedrehte Bilder und leeres Bild. Kein Kameratest auf einem Gerät.
- Logos in Dashboard und Auftragsliste am gleichen linken Rand ausgerichtet, statt innerhalb der verfügbaren Fläche zentriert.
- PDF-Seitenpfeile sind nur noch 48 dp breit. Drucken, Speichern und Teilen stehen in einer kompakten gemeinsamen Leiste ohne breite farbige Flächen.
- Auftrags-PDFs werden in der App aus gespeicherten Daten neu gesetzt: Leistungen mit umbrochenen Texten, danach getrennte Summen. Enthält Kundendaten, Fahrzeug, Barcode, Preise, Rabatt, Unterschrift und vorhandene Vertragshinweise. Weitere Seiten bei Platzmangel. Darstellung unterscheidet sich von der Webvorlage; diese Servervorlage wurde nicht geändert. Briefkopf verwendet Firmenname und Kontaktdaten der gemeinsamen Einstellungen; frei positionierte Briefkopf-Elemente werden nicht übernommen.

25 Java-Dateien kompiliert. Barcode-Tests bestanden. Kamera, Ausrichtung und neu erzeugte Auftrags-PDFs benötigen noch die Prüfung auf einem Android-Gerät.

Barcode-Bibliothek: ZXing Core 3.5.3, Apache License 2.0; Lizenz unter app/libs/ZXing-LICENSE.txt. Projekt: https://github.com/zxing/zxing


## Änderung 1.3.20

Ein Barcode öffnet jetzt das eindeutig zugehörige gespeicherte Formular direkt, unabhängig von der Suchseite, auf der gescannt wurde. RAD-Nummern werden in den Radeinlagerungen nachgeschlagen. Aufträge öffnen ihre Detailseite; Arbeitskarten ihr Formular. Bei mehreren exakten Treffern erscheint eine Auswahl, bei keinem Treffer der Zugang zur Suche. Netzwerkfehler bieten einen erneuten Versuch. Es werden keine Daten verändert.

RAD-9020 wurde per Serverabfrage eindeutig gefunden und anschließend über die gespeicherte ID geladen. Vier Tests prüfen exakte Zuordnung, Groß-/Kleinschreibung und Leerzeichen, fehlende Treffer und doppelte Nummern. Kamera und automatischer Seitenwechsel sind noch nicht auf einem Android-Gerät getestet.


## Änderung 1.3.21

Auf ausdrücklichen Wunsch wird wieder das ursprüngliche Auftrags-PDF des Servers angezeigt, gedruckt, gespeichert und geteilt. Die in 1.3.19 eingeführte abweichende App-Vorlage wird nicht mehr verwendet. Barcode-Scanner, direkte Formularzuordnung und kompakte Vorschau-Bedienung bleiben erhalten. Die bekannte Preisüberlappung in der Servervorlage ist damit noch offen und muss dort ohne Neugestaltung korrigiert werden. Keine Server- oder Kundendaten verändert.
