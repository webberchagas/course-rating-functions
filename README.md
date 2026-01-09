# 📊 Course Rating – Azure Functions

![Java](https://img.shields.io/badge/Java-21-red?logo=openjdk&logoColor=white)
![Azure Functions](https://img.shields.io/badge/Azure%20Functions-Serverless-blue?logo=azurefunctions&logoColor=white)
![Azure](https://img.shields.io/badge/Microsoft%20Azure-Cloud-0078D4?logo=microsoftazure&logoColor=white)
![MongoDB](https://img.shields.io/badge/MongoDB-Database-47A248?logo=mongodb&logoColor=white)
![Service Bus](https://img.shields.io/badge/Azure%20Service%20Bus-Messaging-0089D6?logo=microsoftazure&logoColor=white)
![Maven](https://img.shields.io/badge/Maven-Build-C71A36?logo=apachemaven&logoColor=white)

Este projeto faz parte do **Tech Challenge da Fase 4** e tem como objetivo receber avaliações de cursos/aulas, persistir os dados em banco de dados e **notificar avaliações críticas de forma assíncrona**, utilizando arquitetura **serverless na Azure**.

O serviço foi desenvolvido seguindo boas práticas de:
- separação de responsabilidades
- arquitetura serverless
- comunicação assíncrona
- código limpo e legível

---

## 🎯 Objetivo
Receber avaliações contendo nota e descrição, identificar automaticamente avaliações **críticas**, armazenar os dados e publicar eventos para notificação quando necessário.

Fluxo simplificado:

HTTP Request → Azure Function (POST /ratings) → MongoDB (persistência) →
Service Bus Queue (apenas se avaliação < 6)

---

## ⚙️ Tecnologias Utilizadas

- Java 21
- Azure Functions
- Azure Service Bus
- MongoDB
- Jackson (serialização JSON)
- Maven

---

## 📌 Criar avaliação

**Endpoint**
```
POST /api/ratings
```


**Request body**
```json
{
  "rating": 4,
  "description": "A aula foi confusa",
  "email": "aluno@email.com"
}
```

**Regras**

- rating obrigatório (0 a 10)
- description obrigatória
- avaliações com nota menor que 6 são consideradas críticas

---
## 📥 Persistência

Todas as avaliações são armazenadas no MongoDB com os seguintes dados:

- id
- rating
- description
- email (opcional)
- critical
- createdAt
- updatedAt
- notificationStatus

---

## 📤 Notificação Assíncrona (Service Bus)

- Apenas avaliações críticas são publicadas
- Fila utilizada: critical-ratings
- Tipo: Queue
- Comunicação desacoplada para consumo por serviço de notificação

```json
{
  "id": "uuid",
  "rating": 4,
  "description": "A aula foi confusa",
  "email": "aluno@email.com",
  "critical": true,
  "createdAt": "2025-12-22T18:45:29Z"
}
```
---

## 🔄 Status de Notificação

O status da notificação é atualizado conforme a tentativa de envio:

- ```NOT_REQUIRED``` – Avaliação não crítica
- ```PENDING``` – Avaliação crítica criada
- ```PUBLISHED``` – Evento publicado com sucesso
- ```PUBLISH_FAILED``` – Falha ao tentar publicar o evento

> O status representa tentativa de envio, não processamento do consumidor.

---

## 🔐 Variáveis de Ambiente
**Local** (```local.settings.json```)
```json
{
  "Values": {
    "AzureWebJobsStorage": "UseDevelopmentStorage=true",
    "FUNCTIONS_WORKER_RUNTIME": "java",
    "MONGODB_URI": "...",
    "MONGODB_DB": "...",
    "MONGODB_COLLECTION": "...",
    "QUEUE_CRITICAL_NOTIFICATION": "...",
    "INTERNAL_SECRET_TOKEN": "...",
    "SERVICE_BUS_CONNECTION": "..."
  }
}
```
> Em ambiente Azure, os segredos são referenciados via Azure Key Vault.

---

## ▶️ Executando localmente

1. Configure o ```local.settings.json```
2. Inicie a Function:
```bash
  mvn clean package
  mvn azure-functions:run
```
3. Envie requisição para:
```link
http://localhost:7071/api/ratings
```
