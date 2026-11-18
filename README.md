# Budgeting API com Spring AI

API de orçamento pessoal controlada por **voz**. O usuário envia um áudio (ex.: "gastei 30 reais no almoço"), a IA entende o pedido, executa a ação real no sistema e responde em áudio.

## Fluxo principal

1. O cliente envia um áudio para `POST /api/ai/voice`.
2. `TranscriptionModel` transforma o áudio em texto.
3. `ChatClient` envia o texto ao modelo, que escolhe uma *tool* (`@Tool`).
4. A tool chama um **caso de uso** da camada `application`, que grava ou consulta transações.
5. O texto final da resposta vira áudio WAV (`TextToSpeechModel`).

Os **mesmos casos de uso** atendem o REST (`/api/transactions`) e a IA: a IA nunca acessa o repositório diretamente.

## Arquitetura

```
domain/           Transaction, TransactionId, TransactionType, TransactionRepository (contrato)
application/      CreateTransactionUseCase, ListTransactionsUseCase, GetSummaryUseCase, Period
infrastructure/
  persistence/    Entidade JPA + adapter que implementa TransactionRepository
  web/            Controller REST, DTOs, tratamento de erros
  ai/             BudgetingTools (@Tool), AiAssistantService, VoiceAssistantController
```

## Tecnologias

Java 21 · Spring Boot 3.5 · Spring AI 1.1 (cliente OpenAI apontando para a **Groq**, que é compatível) · Spring Data JPA · H2 (memória) · Gradle · JUnit 5

Modelos (em `application.properties`): chat `openai/gpt-oss-120b`, transcrição `whisper-large-v3-turbo`, voz `canopylabs/orpheus-v1-english`.

## Como executar

Pré-requisitos: JDK 21 e uma chave da [Groq](https://console.groq.com/keys).

```bash
export GROQ_API_KEY="sua_chave_aqui"   # nunca commite a chave
./gradlew bootRun
./gradlew test                          # os testes não usam a Groq/OpenAI
```

No Windows (PowerShell): `$env:GROQ_API_KEY="sua_chave_aqui"; .\gradlew.bat bootRun`

A API sobe em `http://localhost:8080`. O banco é H2 em memória: os dados somem ao reiniciar.

## Como testar o fluxo principal

**1. Fluxo completo com áudio** (grave um arquivo `comando.mp3`, `.wav` ou `.m4a` dizendo, por exemplo, "gastei 45 reais com uber hoje"):

```bash
curl -X POST http://localhost:8080/api/ai/voice \
  -F "audio=@comando.mp3" \
  --output resposta.wav
```

Abra `resposta.wav` para ouvir a resposta. Um exemplo de resposta gerada está em [`docs/exemplo-resposta.wav`](docs/exemplo-resposta.wav).

**2. Mesmo fluxo só com texto** (para depurar as tools sem gravar áudio):

```bash
curl -X POST http://localhost:8080/api/ai/text \
  -H "Content-Type: application/json" \
  -d '{"text":"Recebi 5000 reais de salário"}'

curl -X POST http://localhost:8080/api/ai/text \
  -H "Content-Type: application/json" \
  -d '{"text":"Quanto eu gastei este mês?"}'
```

**3. Conferir pelo REST que a IA realmente gravou no banco:**

```bash
curl http://localhost:8080/api/transactions
curl http://localhost:8080/api/transactions/summary
```

**4. REST direto (sem IA):**

```bash
curl -X POST http://localhost:8080/api/transactions \
  -H "Content-Type: application/json" \
  -d '{"description":"almoço","amount":32.50,"type":"EXPENSE","category":"alimentação"}'
```

## Melhoria implementada: validações antes de salvar a transação

**Limitação identificada.** O projeto base só validava "descrição preenchida" e "valor > 0". Como o comando vem de uma transcrição de áudio e quem preenche os parâmetros da *tool* é o modelo, dados estranhos podiam chegar ao banco: datas no futuro ("amanhã", ano errado), valores absurdos, valores com 3+ casas decimais (que o banco arredondava em silêncio, coluna `scale = 2`) e textos enormes.

**O que mudou.** Agora uma transação só é gravada se passar por estas regras:

| Regra | Onde | Mensagem de erro |
|---|---|---|
| Data não pode ser futura | `CreateTransactionUseCase` | "A data da transação não pode estar no futuro (hoje é ...)" |
| Valor máximo de 10.000.000 | `Transaction` (domínio) | "O valor máximo por transação é ..." |
| No máximo 2 casas decimais | `Transaction` | "O valor deve ter no máximo 2 casas decimais." |
| Descrição até 100 caracteres | `Transaction` | "A descrição deve ter no máximo 100 caracteres." |
| Categoria até 50 caracteres | `Transaction` | "A categoria deve ter no máximo 50 caracteres." |
| Descrição é "trimada" (sem espaços nas pontas) | `Transaction` | n/a |
| Data inválida vinda da IA (ex.: `amanhã`) vira erro claro | `BudgetingTools` | "Data inválida: '...'. Use o formato yyyy-MM-dd." |

**Por que assim.** As regras de negócio ficam no **domínio/caso de uso**, e não na *tool* nem no controller. Assim valem para os dois caminhos (REST e IA) sem duplicar código. No REST, o erro vira `400 Bad Request` pelo `ApiExceptionHandler`. Na IA, a exceção volta para o modelo como resultado da *tool*, e ele pode explicar o problema ao usuário ou corrigir e tentar de novo.

**Arquivos alterados:**
- `domain/Transaction.java`: limites, casas decimais e normalização
- `application/CreateTransactionUseCase.java`: bloqueio de data futura (usa o `Clock` injetado, então é testável)
- `infrastructure/ai/BudgetingTools.java`: erro claro para data em formato inválido
- `test/.../UseCasesTest.java`: 10 novos testes (total de 15)

**Exemplos:**

```bash
# data futura -> 400
curl -X POST http://localhost:8080/api/transactions \
  -H "Content-Type: application/json" \
  -d '{"description":"viagem","amount":100,"type":"EXPENSE","date":"2099-01-01"}'

# 3 casas decimais -> 400
curl -X POST http://localhost:8080/api/transactions \
  -H "Content-Type: application/json" \
  -d '{"description":"teste","amount":10.999,"type":"EXPENSE"}'
```

Resposta esperada (formato `ProblemDetail`):

```json
{"type":"about:blank","title":"Bad Request","status":400,"detail":"O valor deve ter no máximo 2 casas decimais.","instance":"/api/transactions"}
```

Pela voz, algo como *"gastei 20 reais amanhã no cinema"* deve fazer o modelo receber o erro da *tool* e avisar que não é possível lançar uma despesa com data futura.

## Testes

```bash
./gradlew test
```

Os testes de caso de uso usam um repositório em memória (`InMemoryTransactionRepository`) e um `Clock` fixo, então não dependem de banco, rede nem chave de API. Cobrem: data padrão, resumo por categoria, período, e todas as validações da melhoria (incluindo os limites exatos, como valor igual ao máximo e `10.500` com zeros à direita).

## Estrutura de pastas

```
src/main/java/dio/budgeting/...   código (domain, application, infrastructure)
src/test/java/...                 testes
docs/exemplo-resposta.wav         exemplo de resposta em áudio gerada pela IA
```

## O que aprendi

- Como o fluxo **áudio → texto → modelo → tool → caso de uso → texto → áudio** se conecta usando `TranscriptionModel`, `ChatClient` e `TextToSpeechModel` do Spring AI.
- Que o *Tool Calling* só **pede** a execução de uma função: quem decide se a operação é válida é o código da aplicação, não o modelo. Por isso a IA nunca deve acessar o repositório direto.
- Que vale validar no **domínio**: a mesma regra protege REST e IA, e o erro da *tool* volta para o modelo, que consegue se explicar ao usuário.
- Que trocar de provedor (OpenAI → Groq) é só configuração, porque a API é compatível.
- Escrever testes com `Clock` fixo e repositório falso para testar sem depender de rede.
