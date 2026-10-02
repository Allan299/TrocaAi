# TrocaAí

Marketplace acadêmico para estudantes comprarem, venderem, trocarem e doarem materiais escolares e universitários.
Android · Kotlin · Jetpack Compose · Firebase (Authentication, Cloud Firestore, Storage).

## Funcionalidades
- Cadastro, login, logout e sessão mantida ao reabrir o app
- Anúncios de venda, troca ou doação, com foto opcional (galeria → Firebase Storage)
- Editar, excluir e marcar como indisponível (somente o dono)
- Início com anúncios recentes, busca por título, filtros por tipo e categoria
- Detalhes do anúncio e perfil básico do anunciante
- Favoritos por usuário e edição do nome no perfil
- Assistente conversacional baseado em regras locais (sem IA paga)
- Sugestão de preço pela mediana de anúncios semelhantes

## Como executar
1. Android Studio recente, JDK 17+ (o JBR do Android Studio serve) e um emulador ou celular com depuração USB.
2. O arquivo `app/google-services.json` do projeto Firebase deve estar presente (já está neste projeto).
3. No Firebase Console: Authentication → ativar **E-mail/senha**; Firestore → criar o banco e publicar `firestore.rules`;
   Storage (opcional) → ativar e publicar `storage.rules`.
4. Abrir o projeto, aguardar o *Gradle Sync* e executar (▶). Pelo terminal: `./gradlew installDebug`
   (se aparecer erro de `JAVA_HOME`, aponte para `C:\Program Files\Android\Android Studio\jbr`).
5. Testes unitários: `./gradlew testDebugUnitTest`.

## Roteiro de demonstração (≈ 10 min)
Use duas contas (A e B). Deixe o app com o Wi-Fi ligado.
1. **Cadastro e login (A):** "Cadastre-se" → nome, e-mail, senha. Mostrar erros: campos vazios, e-mail inválido, senha curta, e-mail já usado, senha errada no login.
2. **Sessão:** fechar o app pelo gerenciador de tarefas e reabrir: continua logado.
3. **Criar anúncios (A):** aba Anunciar. Criar pelo menos 3 de venda em "Livros" (ex.: "Cálculo Vol. 1" R$ 40, "Livro de Cálculo" R$ 50, "Cálculo Diferencial" R$ 60), um de troca (calculadora científica), um de doação (kit de canetas). Em um deles, escolher foto da galeria.
4. **Sugestão de preço:** novo anúncio de venda em Livros, título "Cálculo 2" → "Sugerir preço" → R$ 50,00 → "Usar este valor". Mostrar o aviso "não é avaliação oficial". Depois escolher uma categoria sem dados → "Sem dados suficientes".
5. **Início e detalhes:** cartões com foto, título, preço e tipo; abrir um anúncio.
6. **Busca e filtros:** digitar "calculo" (sem acento), combinar com chips Venda/Troca/Doação e categoria; termo inexistente → lista vazia.
7. **Editar e excluir (A):** Perfil → Meus anúncios → abrir → Editar (trocar título e foto) → Salvar; marcar como indisponível (some do Início); reativar; excluir com confirmação.
8. **Acesso por dono (B):** entrar com a conta B, abrir um anúncio de A: sem botões Editar/Excluir; botão "Anunciante" mostra o perfil de A.
9. **Favoritos (B):** tocar no coração em dois anúncios → aba Favoritos; remover um. Conta A não vê os favoritos de B.
10. **Perfil:** alterar o nome da conta e mostrar o novo nome nos anúncios.
11. **Assistente:** Início → "Assistente TrocaAí": "livros de programação", "materiais gratuitos", "calculadora científica", "livros de até 50 reais", "violão" (sem resultado), "apague meus anúncios" (recusa). Em um resultado: "Favoritar" → "Não" (nada muda) e depois "Sim" (vai para Favoritos).
12. **Sem conexão:** com a lista aberta, ligar o modo avião, fechar e reabrir o app: aviso "dados salvos no aparelho". Desligar e tocar "Tentar novamente" se necessário.
13. **Regras de segurança (console):** Firestore → Regras → Simulador: `update` ou `delete` em `anuncios/{id}` com outro uid → negado; leitura de `users/{uidA}/favoritos/x` autenticado como B → negada.
14. **Logout:** Perfil → Sair: volta ao login e as telas internas ficam inacessíveis (botão Voltar não reabre).

## Limitações conhecidas
- Busca, filtros, assistente e sugestão de preço usam só os 200 anúncios mais recentes, filtrados no aparelho.
- Estado dos formulários e a conversa do assistente não sobrevivem a girar a tela ou fechar o app.
- Não há chat entre usuários nem contato com o anunciante.
- A foto de um anúncio só pode ser trocada, não removida. Fotos ficam no Storage (pode exigir plano Blaze); sem Storage o app funciona sem fotos.
- O aviso "sem conexão" é inferido do cache do Firestore (aproximação). Escritas feitas offline só sincronizam quando a rede volta.
- Favoritos de anúncios excluídos continuam no banco (apenas ficam ocultos).
- A sugestão de preço não considera o estado de conservação. O assistente só entende o vocabulário previsto nas regras.
- Não há recuperação de senha nem verificação de e-mail. `firestore.rules` permite que qualquer usuário logado leia anúncios e perfis (nome e curso).

## Melhorias futuras (opcionais)
- ViewModel + `rememberSaveable` para preservar estados; paginação de anúncios.
- Busca no servidor (ex.: Algolia/Typesense) ou campo de palavras-chave indexado.
- "Esqueci a senha", verificação de e-mail, login com Google.
- Remover foto, várias fotos por anúncio, cache de imagens configurado.
- Chat ou contato (WhatsApp/e-mail) entre comprador e vendedor; notificações push.
- Substituir o `InterpretadorPorRegras` por um modelo de IA local (a interface `InterpretadorDeConsulta` já isola a troca).
- Ajustar a sugestão de preço por conservação; Cloud Function que limpe favoritos de anúncios excluídos.
- Testes de interface (Compose) e testes das regras com o Firebase Emulator Suite.
- Textos em `strings.xml` (internacionalização) e modo escuro revisado.
