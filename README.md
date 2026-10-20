# coinflip-plugin

Vollständiges Paper-Plugin für ein Spieler-gegen-Spieler-Coinflip mit echten Diamanten als Einsatz.

## Features

- `/coinflip` öffnet eine Inventar-GUI für offene Coinflips
- Coinflip erstellen (Einsatz per Chat-Eingabe, danach GUI-Bestätigung)
- Offene Coinflips anderer Spieler beitreten
- Ersteller kann den eigenen offenen Coinflip per Rechtsklick in der GUI abbrechen
- Serverseitige, atomare Einsatz-Logik mit Schutz gegen doppelte Auszahlungen und Doppelbeitritt
- Sichere Rückerstattung bei Quit, Tod und Server-Neustart (persistente Refund-Ledger-Datei)
- Keine Vault-/Economy-Abhängigkeit, nur echte `DIAMOND`-Items
- Schutz vor GUI-Exploits (Shift-Click, Drag, Hotbar-Swap etc. blockiert)

## Voraussetzungen

- Java 17
- Paper Server (API: `1.20.4-R0.1-SNAPSHOT`)

## Installation

1. Repository klonen
2. Build ausführen:
   ```bash
   mvn clean package
   ```
3. Die erzeugte JAR liegt unter `target/coinflip-plugin-1.0.0.jar`
4. JAR in den `plugins/`-Ordner deines Paper-Servers kopieren
5. Server starten

## Befehl

- `/coinflip` – öffnet die Coinflip-GUI

## Permission

- `coinflip.use` – Nutzung des Coinflip-Plugins

## Konfiguration

Datei: `src/main/resources/config.yml`

- `stakes.min` / `stakes.max`: Minimaler und maximaler Einsatz
- `gui.main.title`: Titel der Haupt-GUI
- `gui.main.list-slots`: Slots, in denen offene Coinflips angezeigt werden
- `gui.buttons.*`: Slots für Erstellen, Seite zurück/weiter, Aktualisieren, Schließen
- `gui.confirm.*`: Slots und Titel für Bestätigungs-GUI
- `messages.*`: Alle Spielertexte/Feldmeldungen

## Sicherheits- und Transaktionshinweise

- Beim Erstellen wird der Einsatz des Erstellers sofort sicher aus dem Inventar entfernt.
- Beim Beitritt wird der Einsatz des Herausforderers geprüft und entfernt.
- Erst danach wird der Gewinner per `SecureRandom` bestimmt.
- Auszahlung erfolgt genau einmal; bei Zustellungsproblemen wird eine sichere Rückerstattung gespeichert und bei Join ausgezahlt.
- Bei Quit, Tod oder Server-Neustart werden offene Einsätze persistiert zurückerstattet.

## Tests

Es existieren Unit-Tests für die Kernlogik (`CoinflipService`):

```bash
mvn test
```
