# Matriz - Requisitos

## Visão e objetivos da aplicação

A aplicação Matriz tem como objetivo apoiar equipas de trabalho na organização de projetos e na gestão das tarefas associadas. A solução pretende centralizar a informação relevante para acompanhar o estado do trabalho, melhorar a coordenação entre elementos da equipa e facilitar o acompanhamento do progresso.

A aplicação deve permitir que os utilizadores criem projetos, adicionem tarefas, acompanhem o estado de execução e mantenham uma visão clara do trabalho em curso.

## Tipos de utilizadores

Os perfis de utilizador, permissões e níveis de acesso ainda não foram definidos e, por isso, não serão assumidos como requisitos neste momento.

## Requisitos funcionais

1. Gestão de projetos
   - O sistema deve permitir criar, consultar, atualizar e remover projetos.
   - Cada projeto deve ter identificação e informação básica de contexto.

2. Gestão de tarefas
   - O sistema deve permitir criar, consultar, atualizar e remover tarefas.
   - Cada tarefa deve estar associada a um projeto.

3. Acompanhamento de progresso
   - O sistema deve permitir acompanhar o estado de cada tarefa.
   - O sistema deve facilitar a visão do estado geral do projeto.

4. Organização do trabalho
   - O sistema deve permitir distinguir tarefas por estado e outra categoria, quando esta for confirmada como requisito do MVP.
   - A prioridade não será tratada como requisito confirmado nesta fase.

5. Pesquisa e consulta
   - O sistema deve permitir localizar tarefas por palavra-chave ou critério simples.

6. Visualização geral
   - O sistema deve disponibilizar uma visão agregada dos projetos e tarefas.

## Requisitos não funcionais

- A aplicação deve ser intuitiva e simples de utilizar.
- A interface deve permitir utilização eficiente por equipas de trabalho.
- O sistema deve apresentar mensagens claras para erros e validações.
- A solução deve ser coerente e previsível na gestão de projetos e tarefas.
- A aplicação deve facilitar organização e rastreabilidade do trabalho.
- A solução deve ser escalável em termos conceptuais para apoiar mais projetos e equipas no futuro.

## Âmbito do MVP

O MVP da Matriz deve incluir:
- criação de projetos;
- criação de tarefas por projeto;
- listagem de projetos e tarefas;
- atualização de estado da tarefa;
- remoção de projetos e tarefas, mediante confirmação do utilizador;
- pesquisa simples por tarefa;
- vista geral do trabalho em curso.

O foco do MVP é a gestão funcional básica e verificável do fluxo de trabalho.

## Funcionalidades fora do âmbito inicial

As seguintes funcionalidades não fazem parte do MVP inicial e não deverão ser tratadas como requisitos essenciais do produto:
- autenticação e gestão de utilizadores avançada;
- múltiplos níveis de permissão;
- notificações automáticas;
- relatórios avançados e dashboards analíticos;
- integração com ferramentas externas;
- comentários e histórico detalhado de alterações;
- gestão de horas, custos ou orçamento;
- IA para apoio à priorização ou organização do trabalho.
