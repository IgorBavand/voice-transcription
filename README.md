
# VoiceTranscribe API 🎙️

API REST para transcrição de áudios, com suporte a arquivos de áudio enviados e transcrição ao vivo via microfone. Desenvolvida com Kotlin e Spring Boot.

## 🚀 Funcionalidades

- 📂 **Transcrição de Arquivos**: Envie arquivos `.wav`, `.mp3`, `.flac`, `.ogg` e receba a transcrição em texto.
- 🔴 **Transcrição ao Vivo**: Suporte a envio de áudio do microfone para transcrição.
- 📜 **Listagem e Consulta**: Liste todas as transcrições ou consulte por ID.
- ❌ **Deleção de Transcrição**.
- 🔎 **Busca de Transcrições por Texto**.

---

## 📦 Requisitos

- Java 17+
- Maven
- PostgreSQL

---

## 🛠️ Tecnologias

- Kotlin 1.9.25
- Spring Boot 3.5.0
- PostgreSQL
- JPA / Hibernate
- WebSockets (preparado)
- Jackson (JSON)
- WebFlux (client reativo)
- JCodec (processamento de áudio)

---

## 🧠 Como Funciona a Transcrição

A transcrição é realizada através da integração com a **API Gemini**, da seguinte forma:

1. 📥 O arquivo de áudio é recebido via `MultipartFile` (upload ou microfone).
2. 🔄 Os bytes do arquivo são convertidos para **Base64**.
3. 🧠 Um `prompt` em português é enviado à API Gemini, solicitando a transcrição do áudio.
4. 🎯 A API retorna somente o texto transcrito (sem comentários adicionais).
5. ⏱️ A duração do áudio é calculada via `AudioSystem` (baseado em `frameLength` e `frameRate`).
6. 💾 O resultado é salvo no banco de dados com informações como:
    - Nome do arquivo
    - Tipo de transcrição (`FILE_UPLOAD` ou `LIVE_RECORDING`)
    - Texto transcrito
    - Duração (em segundos)
    - Tamanho e tipo MIME
    - Confiança (se disponível)

---

## 📁 Endpoints

### `POST /api/transcriptions/transcribe`

Envia um arquivo de áudio para transcrição.

**Parâmetro:** `file: MultipartFile`

**Respostas:**
- ✅ 200 OK: `TranscriptionResponse`
- ❌ 400 Bad Request: `ErrorResponse`

---

### `POST /api/transcriptions/live-transcribe`

Transcreve dados de áudio enviados do microfone.

**Parâmetro:** `audio: MultipartFile`

---

### `GET /api/transcriptions`

Retorna todas as transcrições salvas.

---

### `GET /api/transcriptions/{id}`

Consulta uma transcrição por ID.

---

### `DELETE /api/transcriptions/{id}`

Deleta uma transcrição.

---

### `GET /api/transcriptions/search?query=texto`

Busca transcrições que contenham o texto buscado.

---

## 🧰 Build e Execução

```bash
./mvnw spring-boot:run
```

---


