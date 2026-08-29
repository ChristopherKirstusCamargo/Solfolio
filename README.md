# Solfolio 0.6.3

Aplicativo Android nativo, Material You e local-first para acompanhar portfólios cripto em BRL ou USD. O Solfolio é um tracker somente leitura: não é uma carteira, não movimenta fundos e nunca solicita seed ou chave privada.

## O que mudou na V6

- Migração Room `2 → 3` preserva os dados da V5 e permite o mesmo endereço em portfólios diferentes sem misturar posições.
- Portfólios, operações, custos, endereços e histórico diário ficam no armazenamento privado persistente; apenas preços externos usam cache temporário e limitado.
- A tela inicial aparece imediatamente com o último estado local enquanto preços e saldos são atualizados em segundo plano.
- Gráfico do patrimônio usa o histórico diário do portfólio inteiro, e não o histórico isolado do primeiro ativo.
- Sincronização de carteiras é limitada a três consultas simultâneas para evitar picos de rede e travamentos.
- Ethereum acompanha ETH, USDT e USDC; Solana, Bitcoin e Ethereum continuam independentes por endereço e portfólio.
- P/L manual rejeita vendas acima da quantidade disponível. Endereços rastreados aceitam custo total e custo específico por ativo.
- Análise PRO local: nota explicada, diversificação, concentração, qualidade dos dados, risco, volatilidade e principais exposições.
- Backup PRO criptografado com AES-256-GCM e senha derivada por PBKDF2; a restauração valida referências e valores antes de substituir os dados.
- Compra única PRO preparada para Google Play Billing com o produto `solfolio_pro_lifetime`. O preço exibido vem da Play Store.
- Proteção opcional contra capturas de tela e prévia em aplicativos recentes.
- Estado de navegação e rolagem preservado ao alternar entre telas.
- Área de doação removida da interface principal.

## Free e PRO

O modo gratuito mantém o tracker completo: múltiplas moedas em um portfólio, preços, P/L, gráficos básicos, mercado, funcionamento offline e privacidade local. A V6 reserva para o PRO as ferramentas avançadas de análise, backup/restauração e os recursos premium que forem adicionados depois.

Para publicar, cadastre um produto único não consumível com o identificador `solfolio_pro_lifetime` no Google Play Console. A compra é reconhecida e confirmada pelo Billing Client; o último direito válido é mantido localmente para uso offline. Esta implementação não usa servidor próprio de validação.

## Segurança e privacidade

- O app aceita somente endereços públicos e não possui código de assinatura ou envio de transações.
- Dados permanentes ficam no banco privado do Android. Backup automático do sistema e tráfego HTTP sem TLS permanecem desativados.
- O backup exportado é criptografado, mas sua segurança depende da força da senha escolhida.
- Endereços públicos consultados ficam visíveis aos provedores RPC/API das respectivas redes.
- A proteção de tela, o bloqueio biométrico e o backup reduzem riscos, mas não justificam afirmar que o aplicativo é “100% seguro”.

## Limites honestos

- Preços e saldos dependem de serviços públicos, que podem limitar consultas ou ficar indisponíveis.
- Histórico on-chain não revela o custo real de aquisição; P/L de endereço exige custo informado pelo usuário para ser preciso.
- USDT e USDC Ethereum usam os contratos canônicos da rede principal. Outros ERC-20 ainda podem ser registrados manualmente.
- Notas de risco e cenários são indicadores estatísticos, não previsão nem recomendação financeira.
- A validação de compra é feita no dispositivo. Proteção mais forte contra adulteração exigiria validação externa, com custo e infraestrutura adicionais.

## Compilar e testar

Requisitos: JDK 17, Android SDK 35 e acesso às dependências Gradle.

```bash
./gradlew clean :app:testDebugUnitTest :app:lintDebug :app:assembleDebug
```

O teste instrumentado `Migration2To3Test` exige emulador ou aparelho e confirma que a migração mantém operações e aceita o mesmo endereço em portfólios diferentes.
