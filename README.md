# Guide d'exploitation


## Micro-service pour le matching

### Déploiement via Docker

#### Présentation

Matching_Service est un micro-service d'ordonancement des ordres pour l'application BrokerX. L’application est packagée sous forme d’image Docker, et peut être déployée avec un docker-compose.yml qui lance : - Une base de données PosgreSQL 17  - Le micro-service Spring Boot.

#### Prérequis
	- Docker d'installer sur la machine. (Windows utilisé docker desktop)

#### Configuration
Certaines variables doivent être définies avant le lancement (via un fichier .env) :

| Variable | Description    | Exemple |
|----------|---------------------|----------|
|SPRING_DATASOURCE_USERNAME|Utilisateur DB|postgres|
|SPRING_DATASOURCE_PASSWORD|Mot de passe DB|postgres|

Ces variables sont injectées automatiquement dans le conteneur app par docker-compose.yml

##### Lancement de l'appplication localement
docker compose up --build -d
