# Matriz - Restrições e pressupostos

## Restrições conhecidas

- O cliente ainda não definiu detalhadamente as funcionalidades da aplicação.
- Não existem regras de negócio formalizadas nem critérios de aceitação completos.
- Não foi definido o tipo de interface da aplicação.
- Não estão definidos os tipos de utilizadores, permissões e níveis de acesso.
- Não foi confirmada a necessidade de persistência de dados.
- Não foi definido o nível de maturidade da solução: MVP ou produto completo.

## Pressupostos assumidos

Os seguintes pontos são hipóteses de trabalho e não requisitos confirmados:
- a aplicação será usada por equipas de trabalho para gerir projetos e tarefas;
- cada projeto terá, em princípio, várias tarefas associadas;
- as tarefas podem evoluir ao longo do tempo;
- os utilizadores poderão necessitar de visualizar o estado do trabalho em curso;
- o MVP irá focar-se no núcleo da gestão de projetos e tarefas.

## Riscos e requisitos de segurança e privacidade pendentes de validação

Os seguintes aspetos ainda não foram confirmados pelo cliente, mas representam riscos e requisitos a validar antes do desenvolvimento em ambiente real:
- controlo de acesso aos dados por utilizador ou equipa;
- proteção de dados sensíveis;
- validação de entradas para evitar dados inconsistentes;
- mensagens de erro claras e sem exposição de informação excessiva.

Estes pontos devem ser discutidos com o cliente e confirmados antes de qualquer implementação em ambiente produtivo.

## Decisões técnicas ainda pendentes

- linguagem e framework de desenvolvimento;
- tipo de interface (web, desktop, mobile ou terminal);
- tecnologia de armazenamento de dados;
- nível de autenticação e autorização;
- modelo de dados para projetos, tarefas e utilizadores;
- mecanismos de backup, histórico e recuperação.

## Limitações de âmbito

O projeto, nesta fase, deve manter um escopo controlado e focar-se apenas em:
- criação e gestão de projetos;
- criação e gestão de tarefas;
- consulta e atualização de estado;
- pesquisa simples.

Funcionalidades mais avançadas, como autenticação, permissões, integrações, relatórios avançados ou gestão de recursos, não fazem parte do escopo atual e devem ser discutidas e validadas em fases posteriores.
