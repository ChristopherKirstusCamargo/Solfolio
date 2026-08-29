# Revisão de segurança — Solfolio V6

Data da revisão: 27/08/2026

## Escopo

Revisão estática do manifesto, armazenamento, rede, rastreio watch-only, backup, biometria e compra PRO. Esta revisão não substitui teste dinâmico em aparelho, análise de dependências com banco de vulnerabilidades nem auditoria independente.

## Resultado

| Área | Estado | Observação |
|---|---|---|
| Seed/chave privada | Aprovado | Não existe campo, armazenamento ou fluxo para credenciais de carteira |
| Transações on-chain | Aprovado | Não há assinatura, construção ou envio de transações |
| Permissões | Aprovado | Apenas internet e biometria |
| Tráfego | Aprovado com dependência externa | Endpoints usam HTTPS/WSS; provedores conhecem o IP e o endereço público consultado |
| Dados locais | Aprovado | Room/DataStore no espaço privado; backup automático do Android desativado |
| Captura de tela | Aprovado | `FLAG_SECURE` opcional |
| Backup | Aprovado com ressalva | AES-256-GCM + PBKDF2; a força real depende da senha do usuário |
| Restauração | Aprovado | Limites, validação de referências e substituição transacional |
| PRO | Ressalva conhecida | Entitlement local funciona offline, mas é menos resistente a adulteração que validação em servidor |
| Logs | Aprovado | Não foram encontrados logs de endereços, saldos ou conteúdo do backup |
| HTTP aberto | Aprovado | Política de rede bloqueia cleartext |

## Ameaças consideradas

- Arquivo de backup adulterado: detectado pela autenticação GCM antes da leitura.
- Backup excessivamente grande: leitura e decodificação possuem limites.
- Senha errada: falha antes de qualquer alteração no banco.
- Dados com IDs ou referências quebradas: restauração recusada.
- Falha durante restauração: transação Room evita estado parcialmente substituído.
- RPC fora do ar: o saldo persistido permanece visível e só é substituído após sucesso.
- Mistura de endereços: unicidade composta mantém portfólio, rede e endereço como fonte definida.
- Captura pelo seletor de aplicativos: mitigada quando “Proteger tela” está habilitado.

## Pontos que exigem teste de release

1. Executar `lintRelease` e análise de dependências quando o ambiente Android estiver disponível.
2. Testar migração usando uma cópia real do banco V5, além do teste instrumentado sintético.
3. Validar compra, cancelamento, reembolso e pagamento pendente nas faixas de teste da Play Store.
4. Conferir regras de ProGuard/R8 no APK assinado.
5. Inspecionar o APK final com MobSF ou ferramenta equivalente.
6. Confirmar política comercial de cada provedor de mercado/RPC antes da publicação.

## Conclusão honesta

A arquitetura permanece de baixo risco para um tracker watch-only porque não recebe segredos nem movimenta fundos. Ainda assim, instalar o aplicativo no mesmo aparelho de uma carteira real não torna o tracker capaz de acessar essa carteira: o isolamento do Android continua valendo, salvo comprometimento do aparelho, sistema com root, biblioteca maliciosa futura ou falha externa ainda desconhecida. Portanto, o termo correto é “risco reduzido e escopo somente leitura”, nunca “100% seguro”.
