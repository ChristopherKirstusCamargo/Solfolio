# LARP Wallet

Carteira cenográfica Android para protótipos, histórias, vídeos e demonstrações. A interface é inspirada na fluidez de carteiras modernas, mas usa identidade própria e mantém o aviso **SIMULAÇÃO • SEM VALOR REAL** visível no aplicativo.

## O que o aplicativo faz

- Permite definir qualquer saldo total.
- Permite adicionar e editar ativos, quantidades, preços e variações de 24 horas.
- Simula recebimentos, envios e trocas sem acessar blockchain.
- Mantém um histórico fictício de atividades.
- Permite criar colecionáveis cenográficos.
- Exibe valores em BRL ou USD com cotação manual.
- Salva todo o cenário localmente no aparelho.
- Funciona sem conta e sem internet.

## Limites deliberados

- Não cria ou importa seed phrase.
- Não armazena chaves privadas.
- Não assina nem transmite transações.
- Não consulta saldos, preços ou endereços reais.
- O endereço mostrado na tela de recebimento é propositalmente inválido.
- Não é afiliado à Phantom Technologies, Inc.

O aplicativo não deve ser usado para alegar posse de ativos reais nem para produzir evidências financeiras falsas. O aviso de simulação é uma parte permanente da interface.

## Tecnologia

- Kotlin 2.1.21
- Jetpack Compose + Material 3
- Android 8.0 ou superior (minSdk 26)
- Persistência com `SharedPreferences` e JSON
- Nenhuma permissão de rede

## Compilar

Use Java 17 e o Android SDK 35:

```bash
./gradlew :app:assembleDebug
```

O APK será criado em `app/build/outputs/apk/debug/app-debug.apk`.

## Estrutura

- `model/Models.kt`: ativos, atividades, colecionáveis e estado da carteira.
- `data/WalletRepository.kt`: persistência local.
- `ui/WalletViewModel.kt`: regras das operações simuladas.
- `ui/Screens.kt`: telas principais.
- `ui/WalletSheets.kt`: edição e fluxos de ação.
- `ui/Components.kt`: componentes visuais reutilizáveis.

## Licença

MIT. By Christopher.

