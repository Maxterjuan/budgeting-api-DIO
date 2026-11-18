# Budgeting API com Spring AI

Projeto do desafio da DIO com Spring Boot e Spring AI. É uma API de orçamento que funciona por voz: eu mando um áudio falando, por exemplo, "gastei 30 reais no almoço", a IA entende, salva a transação e responde em áudio.

## O que o projeto faz

1. Recebe um áudio
2. Transforma o áudio em texto
3. A IA entende o que foi pedido e chama uma função da aplicação
4. A função cria ou consulta as transações
5. A resposta volta em áudio

## Melhoria que eu fiz

Adicionei validações antes de salvar uma transação. Percebi que a IA pode errar o que entendeu do áudio, então a API agora não aceita:

- data no futuro
- valor maior que 10 milhões
- valor com mais de 2 casas decimais
- descrição com mais de 100 caracteres
- categoria com mais de 50 caracteres

Também deixei a mensagem de erro mais clara quando a IA manda uma data em formato errado.

Coloquei as regras em `Transaction.java` e `CreateTransactionUseCase.java` (e um ajuste em `BudgetingTools.java`) para valer tanto para a IA quanto para o REST. Também criei 10 testes novos em `UseCasesTest.java`.

## Tecnologias

Java 21, Spring Boot, Spring AI, Spring Data JPA, H2, Gradle e JUnit 5. Usei a Groq como provedor de IA.

## Como executar

Precisa do JDK 21 e de uma chave da Groq (https://console.groq.com/keys).

```
export GROQ_API_KEY="sua_chave"
./gradlew bootRun
```

No Windows (PowerShell):

```
$env:GROQ_API_KEY="sua_chave"
.\gradlew.bat bootRun
```

A API fica em http://localhost:8080. A chave não está no código, ela vem de variável de ambiente.

## Como testar

Com áudio (use um arquivo seu, tipo `comando.mp3`):

```
curl -X POST http://localhost:8080/api/ai/voice -F "audio=@comando.mp3" --output resposta.wav
```

Um exemplo de resposta da IA está em `docs/exemplo-resposta.wav`.

Só com texto, para testar sem gravar:

```
curl -X POST http://localhost:8080/api/ai/text -H "Content-Type: application/json" -d '{"text":"Recebi 5000 reais de salário"}'
```

Para ver o que foi salvo:

```
curl http://localhost:8080/api/transactions
```

Para testar a melhoria (deve dar erro 400 porque a data é futura):

```
curl -X POST http://localhost:8080/api/transactions -H "Content-Type: application/json" -d '{"description":"viagem","amount":100,"type":"EXPENSE","date":"2099-01-01"}'
```

Para rodar os testes automáticos:

```
./gradlew test
```

## O que eu aprendi

Aprendi como conectar uma IA com uma aplicação Java de verdade. A IA não salva nada sozinha, ela só pede para o código executar uma função. Aprendi também que não dá para confiar 100% no que a IA entende, então é importante validar os dados antes de salvar. Além disso, aprendi a fazer testes sem precisar da internet nem da chave da IA.
