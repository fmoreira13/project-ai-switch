# Matriz - User Stories

## User Stories do MVP

### US-01
Como utilizador, quero criar um projeto, para organizar o trabalho em torno de um objetivo comum.

Critérios de aceitação:
- O sistema permite criar um projeto com nome válido.
- O projeto é armazenado e listado corretamente.
- O sistema rejeita criação sem nome ou com dados inválidos.
- O sistema impede a criação de um projeto com o mesmo nome de um projeto já existente.

Casos excecionais:
- projeto com nome duplicado;
- projeto sem nome;
- tentativa de criar projeto com dados vazios.

### US-02
Como utilizador, quero adicionar tarefas a um projeto, para distribuir o trabalho necessário para atingir os objetivos definidos.

Critérios de aceitação:
- O sistema permite criar uma tarefa associada a um projeto existente.
- O sistema rejeita tarefas sem título ou com dados mínimos inválidos.
- A tarefa aparece na lista de tarefas do projeto correto.
- A tarefa deve permanecer associada ao projeto correto após a criação.

Casos excecionais:
- projeto inexistente;
- tarefa sem título;
- tarefa criada para projeto não selecionado ou inválido.

### US-03
Como utilizador, quero visualizar os projetos e as tarefas associadas, para conhecer o trabalho em curso e o seu estado atual.

Critérios de aceitação:
- O sistema apresenta a lista de projetos disponíveis.
- Ao selecionar um projeto, o sistema apresenta as tarefas associadas.
- Cada tarefa apresentada deve mostrar o seu estado atual, com um valor permitido pela definição do sistema.

Casos excecionais:
- lista de projetos vazia;
- projeto sem tarefas;
- projeto inexistente ao consultar detalhes.

### US-04
Como utilizador, quero atualizar o estado de uma tarefa, para refletir o progresso real do trabalho.

Critérios de aceitação:
- O sistema permite alterar o estado da tarefa para um valor válido.
- A atualização é refletida imediatamente na apresentação da tarefa.
- O sistema rejeita valores inválidos ou desconhecidos.

Casos excecionais:
- tentativa de atualizar tarefa inexistente;
- estado não suportado;
- alteração de tarefa em projeto inexistente.

### US-05
Como utilizador, quero remover um projeto ou uma tarefa, para manter o espaço de trabalho organizado e alinhado com o contexto atual.

Critérios de aceitação:
- O sistema elimina o projeto ou tarefa apenas após confirmação do utilizador.
- O sistema informa o utilizador quando a ação não é possível.
- A remoção não deve afetar outros projetos ou tarefas sem confirmação explícita.

Casos excecionais:
- tentativa de remover tarefa inexistente;
- tentativa de remover projeto inexistente;
- operação sem confirmação por parte do utilizador.

### US-06
Como utilizador, quero procurar tarefas por palavra-chave, para encontrar rapidamente o trabalho relevante.

Critérios de aceitação:
- O sistema permite procurar por termo de texto.
- Os resultados apresentam tarefas que correspondem à pesquisa.
- Se não existir correspondência, o sistema informa claramente que não encontrou resultados.

Casos excecionais:
- pesquisa vazia;
- termo sem correspondência;
- múltiplos resultados com similaridade parcial.

## Critérios gerais de aceitação

- Cada User Story deve ser verificável de forma objetiva.
- Cada funcionalidade deve ter um comportamento claro em caso de erro.
- O backlog deve manter apenas requisitos confirmados e relevantes para o MVP.
