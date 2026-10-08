# OpsServiceDoc

Support-Workflow und IPD-Dokumentgenerator für Wartungseinsätze – Abschlussprojekt im Java-Bootcamp bei neue fische.

Die Anwendung begleitet einen Einsatz von Anfang bis Ende: Tickets kommen aus **GLPI**, die einzelnen Arbeitsschritte werden im **TaskPlanner** erfasst, die interne Qualitätssicherung läuft über **Checklisten**, und am Ende entsteht daraus ein fertiges **IPD-Dokument** (Infrastructure Planning & Design) als PDF für den Kunden.

## Inhalt

- [Funktionen](#funktionen)
- [Technik](#technik)
- [Voraussetzungen](#voraussetzungen)
- [Lokal starten](#lokal-starten)
- [Konfiguration](#konfiguration)
- [Tests und Qualität](#tests-und-qualität)
- [API-Überblick](#api-überblick)
- [Sicherheit](#sicherheit)
- [Projektstruktur](#projektstruktur)
- [Geplante Erweiterungen](#geplante-erweiterungen)

## Funktionen

| Bereich | Was es kann |
| --- | --- |
| **Tickets** | Tickets aus GLPI synchronisieren, ansehen und bearbeiten (Status, Techniker, Szenario). |
| **TaskPlanner** | Arbeitsschritte zu einem Ticket erfassen, Status setzen und abhaken. |
| **Checklisten** | Checklisten manuell oder aus Vorlagen anlegen, Punkte abhaken, Vorlagen verwalten (Standardvorlagen sind vor dem Löschen geschützt). |
| **IPD-Generator** | IPD-Dokument aus einem Ticket erzeugen. Erledigte Tasks und der Stand der Qualitätssicherung werden automatisch übernommen. Export als PDF im DahlLab-Design. |
| **Checklisten-PDF** | Interne Technikerversion der Checkliste mit ausfüllbaren Checkboxen, zum Ausdrucken oder Bearbeiten am Tablet. Getrennt vom Kunden-PDF. |
| **GLPI-Link** | Navigation und Startseite führen direkt in die GLPI-Weboberfläche (neuer Tab). |

Das Kunden-PDF zeigt von der Checkliste bewusst nur das Ergebnis („Qualitätssicherung durchgeführt: Ja/Nein“). Die Details bekommt nur der Techniker über das separate Checklisten-PDF.

## Technik

**Backend** (`backend/`)
- Java 25, Spring Boot 4.1
- Spring Web MVC, Spring Security (OAuth2-Login mit GitHub), Bean Validation
- Spring Data MongoDB
- OpenPDF für die PDF-Erzeugung
- springdoc-openapi (Swagger UI)
- JUnit 5, Mockito, MockMvc, JaCoCo

**Frontend** (`frontend/`)
- React 19, TypeScript, Vite
- React Router, React-Bootstrap

**Infrastruktur**
- MongoDB 7, GLPI und die zugehörige MySQL-Datenbank laufen per Docker Compose
- GitHub Actions mit SonarCloud-Analyse für Backend und Frontend

## Voraussetzungen

- JDK 25
- Node.js (aktuelle LTS-Version) und npm
- Docker mit Docker Compose
- Eine GitHub OAuth App (siehe unten)

## Lokal starten

### 1. Umgebungsvariablen anlegen

Im Projektstamm eine Datei `.env` anlegen (sie steht in der `.gitignore` und wird nie committet). Welche Variablen es gibt, steht unter [Konfiguration](#konfiguration).

### 2. Datenbanken und GLPI starten

```bash
docker compose up -d
```

Das startet MongoDB (Port 27017), GLPI (Port 80) und die GLPI-Datenbank.

GLPI muss beim ersten Start über den Web-Installer unter `http://localhost` eingerichtet werden. Danach in GLPI die REST-API aktivieren und einen App-Token sowie einen User-Token erzeugen. Beide kommen in die `.env`.

### 3. GitHub OAuth App anlegen

Unter GitHub → Settings → Developer settings → OAuth Apps eine neue App erstellen:

- Homepage URL: `http://localhost:5173`
- Authorization callback URL: `http://localhost:8080/login/oauth2/code/github`

Client-ID und Client-Secret kommen in die `.env`.

### 4. Backend starten

Die Variablen aus der `.env` müssen im Backend-Prozess als Umgebungsvariablen ankommen (in IntelliJ unter den Run-Einstellungen der Anwendung).

```bash
cd backend
./mvnw spring-boot:run        # Windows: mvnw.cmd spring-boot:run
```

Das Backend läuft auf `http://localhost:8080`. Die API-Dokumentation liegt unter `http://localhost:8080/swagger-ui.html`.

### 5. Frontend starten

```bash
cd frontend
npm install
npm run dev
```

Das Frontend läuft auf `http://localhost:5173` und leitet Aufrufe an `/api` per Vite-Proxy ans Backend weiter. Wichtig: Alle npm-Befehle gehören in den Ordner `frontend/`.

## Konfiguration

Alle Zugangsdaten kommen aus Umgebungsvariablen. In den Quelltext gehören nie echte Tokens oder Passwörter.

| Variable | Pflicht | Bedeutung |
| --- | --- | --- |
| `GITHUB_CLIENT_ID` | ja | Client-ID der GitHub OAuth App |
| `GITHUB_CLIENT_SECRET` | ja | Client-Secret der GitHub OAuth App |
| `GLPI_API_URL` | ja | REST-API von GLPI, z. B. `http://localhost/api.php/v1` |
| `GLPI_APP_TOKEN` | ja | App-Token der GLPI-API |
| `GLPI_USER_TOKEN` | ja | User-Token der GLPI-API |
| `GLPI_WEB_URL` | nein | Adresse der GLPI-Weboberfläche für den Link im Frontend. Standard: `http://localhost` |
| `MONGODB_URI` | nein | MongoDB-Verbindung. Standard ist die lokale Entwicklungsdatenbank |
| `FRONTEND_URL` | nein | Adresse des Frontends für die Weiterleitung nach Login und Logout. Standard: `http://localhost:5173` |
| `GLPI_DB_HOST`, `GLPI_DB_NAME`, `GLPI_DB_USER`, `GLPI_DB_PASSWORD`, `GLPI_DB_PORT` | für Docker Compose | Zugang der GLPI-Datenbank |

Der GLPI-Link im Frontend erscheint nur, wenn das Backend eine gültige `http(s)`-Adresse kennt.

## Tests und Qualität

```bash
# Backend (benötigt eine laufende MongoDB, siehe docker compose)
cd backend
./mvnw test

# Frontend
cd frontend
npm run build
npm run lint
```

- Die Backend-Tests laufen gegen eine eigene Test-Datenbank (`ops_service_doc_test`), damit die Entwicklungsdaten nicht berührt werden.
- Controller-Tests nutzen MockMvc mit simuliertem GitHub-Login, Service-Tests arbeiten mit Mockito.
- JaCoCo erzeugt die Testabdeckung, SonarCloud wertet sie in der CI aus. Der Workflow läuft bei jedem Push auf `master` und bei jedem Pull Request.

## API-Überblick

Alle Endpoints liegen unter `/api` und verlangen einen Login. Die vollständige, interaktive Dokumentation steht in der Swagger UI.

| Pfad | Zweck |
| --- | --- |
| `GET /api/auth/me` | Angemeldeten GitHub-Nutzer abfragen |
| `/api/tickets` | Tickets verwalten, `POST /api/tickets/sync-glpi` synchronisiert aus GLPI |
| `/api/tasks` | Tasks zu Tickets |
| `/api/checklists` | Checklisten, `POST /api/checklists/from-template` erzeugt sie aus einer Vorlage |
| `/api/checklist-templates` | Checklisten-Vorlagen |
| `/api/ipd` | IPD-Dokumente, `POST /api/ipd/from-ticket/{ticketId}` erzeugt einen Entwurf |
| `GET /api/ipd/{id}/pdf` | Kunden-PDF |
| `GET /api/ipd/{id}/checklist-pdf` | Checklisten-PDF für den Techniker |
| `GET /api/config/glpi-url` | Adresse der GLPI-Weboberfläche |

## Sicherheit

- **Login:** OAuth2 mit GitHub, danach Session-Cookie.
- **Zugriffsschutz:** `/api/**` ist insgesamt geschützt. Neue Controller sind damit automatisch abgesichert. Ohne Login antwortet die API mit `401`.
- **CSRF-Schutz:** Schreibende Aufrufe brauchen ein CSRF-Token. Das Backend legt es als Cookie `XSRF-TOKEN` ab, das Frontend schickt es im Header `X-XSRF-TOKEN` zurück (siehe `frontend/src/api/api.ts`). Das Logout-Formular sendet es als verstecktes Feld.
- **Geheimnisse:** Tokens und Passwörter stehen ausschließlich in Umgebungsvariablen bzw. der nicht versionierten `.env`.

## Projektstruktur

```
ops-service-doc/
├── backend/                 Spring-Boot-Anwendung
│   └── src/main/java/org/dahllab/opsservicedoc/
│       ├── config/          GLPI-Konfiguration, Seeder für Standardvorlagen
│       ├── controller/      REST-Endpoints
│       ├── dto/             Datenobjekte der API
│       ├── exception/       Zentrale Fehlerbehandlung
│       ├── model/           MongoDB-Dokumente
│       ├── repository/      Spring-Data-Repositories
│       ├── security/        Login und Security-Konfiguration
│       ├── service/         Fachlogik, GLPI-Client
│       └── util/            Mapper und PDF-Generatoren
├── frontend/                React-Anwendung
│   └── src/
│       ├── api/             Fetch-Hilfsfunktionen und Typen
│       ├── auth/            Login-Zustand
│       ├── components/      Wiederverwendbare Bausteine
│       ├── hooks/           Eigene Hooks
│       ├── pages/           Seiten (Tickets, TaskPlanner, Checklisten, IPD)
│       └── utils/           Formatierung und Download-Helfer
├── docker-compose.yml       MongoDB, GLPI, GLPI-Datenbank
└── .github/workflows/       CI mit SonarCloud
```

## Geplante Erweiterungen

- Doppelte Tickets beim wiederholten GLPI-Sync vermeiden
- Frontend-Tests für Hilfsfunktionen und Seiten
- Weitere Tests für Verzweigungen in den Services
- Weitere Wartungsszenarien und Checklistenvorlagen
