# Decisões de engenharia — Solfolio V6

## Direção adotada

A V6 continua sobre a aplicação V5, o mesmo `applicationId`, o mesmo banco e a mesma navegação principal. A evolução foi concentrada em persistência, cálculo, segurança, desempenho e acabamento. Nenhuma tela funcional foi recriada apenas por preferência arquitetural.

## Arquitetura de dados

| Dado | Armazenamento | Regra |
|---|---|---|
| Portfólios, operações e ativos | Room | Permanente e local |
| Endereços e saldos rastreados | Room | Cada endereço permanece ligado a um portfólio e uma rede |
| Custo por endereço/ativo | Room | Usado para aumentar a cobertura e a precisão do P/L |
| Histórico diário | Room | Um ponto por portfólio/dia; retenção de 370 dias |
| Preferências | DataStore | Tema, paleta, moeda, privacidade e navegação |
| Cotações | Cache privado temporário | Máximo de 80 ativos; pode ser descartado sem perder o portfólio |
| Backup | Arquivo escolhido pelo usuário | AES-256-GCM, PBKDF2-HMAC-SHA256 e senha local |

A migração Room `2 → 3` remove a unicidade global do endereço e cria a regra `(portfolioId, network, address)`. Assim, posições não são fundidas entre carteiras e o mesmo endereço pode ser acompanhado em mais de um portfólio quando essa for a escolha do usuário.

## Cálculo e análise

- Operações manuais continuam usando custo médio móvel e P/L realizado/não realizado.
- Uma venda maior que a quantidade manual disponível é rejeitada antes de ser persistida.
- Endereços podem receber custo total ou custo específico por ativo. Custos específicos têm prioridade; o restante é distribuído proporcionalmente apenas entre os ativos sem custo informado.
- O gráfico principal usa snapshots do patrimônio completo. Enquanto o histórico diário ainda está sendo construído, a curva curta agrega os movimentos recentes de todas as posições.
- A análise detalhada é determinística, gratuita e local. Utiliza concentração HHI, quantidade efetiva de ativos, cobertura de preço/custo, exposição principal e volatilidade histórica quando há pelo menos sete pontos.
- Toda nota possui explicações. Os textos descrevem os dados e não fazem promessa ou recomendação financeira.

## Performance

- Um único `OkHttpClient` é compartilhado entre serviços.
- A tela usa imediatamente os dados locais e atualiza rede em segundo plano.
- A atualização de endereço só substitui o saldo persistido depois de uma resposta bem-sucedida.
- No máximo quatro endereços são sincronizados ao mesmo tempo.
- Estado e rolagem de cada destino são preservados.
- Cálculos intermediários agrupam operações e endereços uma vez por emissão, evitando filtros repetidos.
- Animações de navegação são curtas e usam transformações leves.

## Serviços externos e custo

A cotação usa o WebSocket público do Coinbase Advanced Trade, com uma única conexão ativa apenas enquanto o aplicativo está em primeiro plano. Uma consulta REST por minuto preenche ativos sem ticker recebido. As condições de Market Data podem mudar e devem ser revistas antes de cada publicação ampla.

## Segurança e privacidade

- O aplicativo continua watch-only e não contém fluxo de seed, chave privada, assinatura ou envio.
- `allowBackup=false`, tráfego HTTP aberto bloqueado e armazenamento privado foram preservados.
- `FLAG_SECURE` é opcional para impedir captura e prévia no seletor de aplicativos.
- O backup autentica o conteúdo com AES-GCM, limita tamanho e quantidade de registros e valida todas as referências antes da transação de restauração.
- A biometria não é importada de um backup.

## Produto gratuito

Tracker, preços, P/L, mercado, análise detalhada, backup protegido, privacidade e uso offline estão disponíveis para todos. Doações são opcionais e não liberam funções.

## Testes obrigatórios antes da publicação

| Cenário | Resultado esperado |
|---|---|
| Atualização V5 → V6 | Operações e endereços preservados |
| Mesmo endereço em dois portfólios | Duas fontes independentes, sem erro de unicidade |
| Sem internet | Último patrimônio aparece; atualização informa indisponibilidade |
| Internet lenta | Interface continua navegável; saldo local não desaparece |
| Venda acima do saldo | Operação recusada |
| Backup com senha errada | Autenticação falha sem alterar o banco |
| Backup válido | Restauração atômica e referências preservadas |
| Um ativo | Métricas sem divisão inválida |
| Muitos ativos/operações | Rolagem e navegação responsivas |
| Preço de compra vazio | Posição permanece visível sem inventar P/L |

## Itens deliberadamente não adicionados

- IA paga: contrariaria o funcionamento local e o custo próximo de zero.
- Servidor próprio: aumentaria custo, coleta de dados e superfície de ataque.
- Notícias como requisito da análise: contexto de mercado permanece separado do diagnóstico do portfólio.
- Sincronização agressiva em segundo plano: aumentaria consumo de rede e bateria.
- Afirmação de segurança absoluta ou previsão de retorno: seria tecnicamente incorreta.
