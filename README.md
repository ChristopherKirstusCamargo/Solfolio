# Solfolio 0.7.0

O Solfolio é somente leitura: não movimenta fundos e nunca solicita seed ou chave privada.

## Atualização 0.7.0

- Aplicativo gratuito.
- Doações opcionais não liberam funções.
- Backup e restauração criptografados disponíveis para todos.
- Gráfico histórico interativo: toque ou deslize para consultar preço e data.
- 40 criptoativos no catálogo e no mercado, com fallback de preços via Coinbase.
- 26 moedas fiat de exibição com câmbio atualizado e taxas offline de contingência.
- Posições manuais podem ser removidas; preço de compra pode ficar em branco.
- Valores grandes se ajustam ao espaço sem quebrar a tela.
- Análise local reorganizada, com explicações curtas e métricas mais legíveis.
- Bloqueio biométrico configurável: imediato, 1, 5 ou 10 minutos.
- Indicador inferior desliza entre as abas e o último destino continua salvo.
- Dados locais aparecem antes da sincronização; preços e carteiras atualizam em segundo plano.

## Distribuição

O APK pode ser distribuído gratuitamente pela página de Releases deste repositório. Android poderá pedir ao usuário autorização para instalar aplicativos dessa fonte. Publique também o SHA-256 do arquivo em cada release e use assinatura de lançamento própria antes de distribuição ampla.

## Segurança e privacidade

- Aceita apenas endereços públicos e não possui código de assinatura ou envio de transações.
- Dados permanentes ficam no banco privado do Android; tráfego HTTP sem TLS e backup automático do sistema estão desativados.
- Backups exportados usam AES-256-GCM com chave derivada por PBKDF2.
- Serviços de preço e RPC recebem os endereços públicos consultados.
- Proteção de tela e biometria são opcionais.
