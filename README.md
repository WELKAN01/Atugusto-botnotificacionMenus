# Notify

Backend Spring Boot para gestionar platos disponibles y responder mensajes de WhatsApp mediante la Meta Graph API. El proyecto combina endpoints REST, persistencia con PostgreSQL, envio de listas interactivas por WhatsApp y una integracion inicial con Spring AI.

## Cambios y funcionalidades actuales

- Se agrego una funcionalidad de mensajeria de texto consumiendo Meta API / WhatsApp Cloud API.
- Se implemento un webhook en `/webhook` para recibir eventos de WhatsApp.
- Se agrego verificacion del webhook de Meta mediante `hub.mode`, `hub.verify_token` y `hub.challenge`.
- Se parsea el payload entrante de WhatsApp usando DTOs tipados.
- Se detectan mensajes de texto recibidos y se responde al usuario con una lista interactiva de platos disponibles.
- Se agrego soporte para payloads interactivos de WhatsApp, incluyendo `list_reply` y `button_reply`.
- Se creo la entidad `Platos` con nombre, categoria, descripcion, precio y disponibilidad.
- Se agrego persistencia con Spring Data JPA y PostgreSQL.
- Se agregaron endpoints REST para listar y crear platos.
- Se agrego una carga inicial de datos con Datafaker cuando la tabla de platos esta vacia.
- Se agrego integracion con Spring AI mediante `ChatClient`.
- Se agrego una herramienta MCP/Spring AI para consultar la cantidad de platos registrados.
- Se configuro `WebClient` para realizar llamadas HTTP salientes hacia la Graph API.

## Tecnologias

- Java 17
- Spring Boot 3.3.5
- Spring WebFlux
- Spring Data JPA
- PostgreSQL
- Spring AI 1.0.0
- Meta Graph API / WhatsApp Cloud API
- Lombok
- Jackson
- Datafaker
- Maven

## Estructura principal

```text
src/main/java/com/atugusto/notify
+-- Controller
|   +-- Platoscontroller.java
|   +-- WhatsappHookController.java
|   +-- mcp/tools/PlatosToolsMCP.java
+-- DTO
|   +-- WebhookWhatsapp.java
|   +-- messageTO.java
+-- Entity
|   +-- Platos.java
+-- Repository
|   +-- PlatosRepository.java
+-- Service
|   +-- MessageService.java
|   +-- PlatosService.java
|   +-- IAService/IAService.java
+-- config
    +-- Datafaker.java
    +-- WebConfig.java
```

## Configuracion

La configuracion actual esta en `src/main/resources/application.properties`:

```properties
spring.application.name=notify
server.port=${SERVER_PORT:8090}
spring.r2dbc.url=${SPRING_R2DBC_URL:r2dbc:postgresql://localhost:5432/atugusto}
spring.r2dbc.username=${SPRING_R2DBC_USERNAME:postgres}
spring.r2dbc.password=${SPRING_R2DBC_PASSWORD:123456}
meta.whatsapp.token=${META_WHATSAPP_TOKEN:}
meta.whatsapp.verify-token=${META_WHATSAPP_VERIFY_TOKEN:mi_token_seguro}
```

Antes de ejecutar el proyecto, asegurate de tener una base de datos PostgreSQL llamada `atugusto` disponible localmente.

> Nota: las credenciales de base de datos y el token de verificacion del webhook tambien deberian moverse a variables de entorno o secretos antes de usar el proyecto en produccion.

### Configurar tokens y secretos

El token de WhatsApp Cloud API se lee directamente desde la variable de entorno `META_WHATSAPP_TOKEN`.
El token de verificacion del webhook se puede configurar con `META_WHATSAPP_VERIFY_TOKEN`.

En PowerShell:

```powershell
$env:META_WHATSAPP_TOKEN="TU_TOKEN_DE_META"
$env:META_WHATSAPP_VERIFY_TOKEN="mi_token_seguro"
```

En CMD:

```cmd
set META_WHATSAPP_TOKEN=TU_TOKEN_DE_META
set META_WHATSAPP_VERIFY_TOKEN=mi_token_seguro
```

Luego ejecuta la aplicacion desde esa misma terminal.

## Ejecucion

Desde la raiz del proyecto:

```bash
./mvnw spring-boot:run
```

En Windows:

```bash
mvnw.cmd spring-boot:run
```

La aplicacion levanta por defecto en:

```text
http://localhost:8090
```

## Docker

La forma mas simple de levantar todo el stack es con Docker Compose. Se incluyen:

- una imagen de aplicacion basada en Java 17
- una imagen de PostgreSQL 16
- un script de inicializacion SQL para crear las tablas `platos` y `platos_diarios`

### Variables de entorno

Puedes copiar `.env.example` a `.env` y completar los valores sensibles:

```env
APP_PORT=8090
POSTGRES_PORT=5432
POSTGRES_DB=atugusto
POSTGRES_USER=postgres
POSTGRES_PASSWORD=123456
META_WHATSAPP_TOKEN=TU_TOKEN_DE_META
META_WHATSAPP_VERIFY_TOKEN=mi_token_seguro
OPENAI_API_KEY=
```

### Levantar servicios

```bash
docker compose up --build
```

La API quedara disponible en:

```text
http://localhost:8090
```

### Detener servicios

```bash
docker compose down
```

Si tambien quieres eliminar el volumen de datos de PostgreSQL:

```bash
docker compose down -v
```

## Exponer webhook local con ngrok

Para que Meta pueda enviar eventos al webhook mientras el proyecto corre localmente, se puede exponer el puerto `8090` usando `ngrok`.

1. Ejecutar la aplicacion:

```bash
./mvnw spring-boot:run
```

En Windows:

```bash
mvnw.cmd spring-boot:run
```

2. En otra terminal, iniciar `ngrok` apuntando al puerto del proyecto:

```bash
ngrok http 8090
```

3. Copiar la URL HTTPS generada por `ngrok`. Ejemplo:

```text
https://abc123.ngrok-free.app
```

4. Registrar en Meta Developers la URL del webhook agregando el path `/webhook`:

```text
https://abc123.ngrok-free.app/webhook
```

5. Usar como token de verificacion:

```text
mi_token_seguro
```

Ese token debe coincidir con el valor configurado en `WhatsappHookController`.

> Nota: si usas la version gratuita de `ngrok`, la URL cambia cada vez que reinicias el tunel. Cuando cambie, tambien debes actualizar la URL del webhook en Meta.

## Endpoints disponibles

### Platos

#### Listar platos disponibles

```http
GET /platos
```

Devuelve los platos cuyo campo `disponible` sea `true`.

#### Crear plato

```http
POST /platos
Content-Type: application/json
```

Ejemplo:

```json
{
  "nombre": "Lomo saltado",
  "categoria": "PRINCIPAL",
  "descripcion": "Plato criollo con carne, papas y arroz",
  "precio": 32.5,
  "disponible": true
}
```

Categorias soportadas actualmente:

```text
ENTRANTE
PRINCIPAL
```

#### Consultar IA

```http
GET /platos/ai/cantidad
Content-Type: application/json
```

Ejemplo:

```json
{
  "message": "Cuantos platos hay disponibles?"
}
```

Este endpoint envia el mensaje recibido a `IAService`, que usa `ChatClient` de Spring AI.

### Webhook de WhatsApp

#### Verificar webhook

```http
GET /webhook?hub.mode=subscribe&hub.verify_token=mi_token_seguro&hub.challenge=CHALLENGE
```

Si el token coincide, responde el valor de `hub.challenge`, como requiere Meta.

#### Recibir eventos

```http
POST /webhook
Content-Type: application/json
```

El controlador procesa el payload de WhatsApp, extrae:

- `phone_number_id`
- numero del remitente
- texto del mensaje
- estados del mensaje
- respuestas interactivas, si existen

Cuando recibe un mensaje de texto valido, llama a `MessageService` para responder al usuario con una lista interactiva de platos disponibles.

## Flujo de mensajeria

1. Meta envia un evento al endpoint `POST /webhook`.
2. `WhatsappHookController` interpreta el payload usando `WebhookWhatsapp`.
3. Si el evento contiene un mensaje de texto, se construye un `messageTO`.
4. `MessageService` consulta los platos disponibles mediante `PlatosService`.
5. Se genera una lista interactiva de WhatsApp con los platos disponibles.
6. Se envia la respuesta a:

```text
https://graph.facebook.com/v25.0/{phone_number_id}/messages
```

## Base de datos y datos iniciales

La entidad principal es `Platos`:

```text
id
nombre
categoria
descripcion
precio
disponible
```

`Datafaker` carga 10 platos automaticamente cuando la tabla esta vacia.

## Notas tecnicas pendientes

- Mover tokens y credenciales hardcodeadas a variables de entorno.
- Ajustar `Datafaker`, porque actualmente puede generar la categoria `POSTRE`, pero el enum `Platos.Categoria` solo define `ENTRANTE` y `PRINCIPAL`.
- Revisar el uso de `GET /platos/ai/cantidad` con body JSON; por convencion HTTP podria ser `POST` si requiere cuerpo.
- Agregar pruebas unitarias o de integracion para controladores, servicios y webhook.
- Mejorar el manejo de errores cuando falla la llamada a Meta API.
- Evitar imprimir payloads completos en consola si contienen informacion sensible.

## Comandos utiles

Compilar:

```bash
./mvnw clean compile
```

Ejecutar pruebas:

```bash
./mvnw test
```

Ejecutar aplicacion:

```bash
./mvnw spring-boot:run
```
