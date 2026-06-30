# Dux Scholar
Um *proof of concept* para um aplicativo voltado a instituições de ensino. Configure o conteúdo do app sem sair dele.

## *Features*
* Painel de configuração para administradores
  * Cadastre alunos e professores, adicione notícias, informações acadêmicas, cursos, disciplinas e mais.
* Mascote *Chatbot* auxiliar que atualiza seus conhecimentos conforme adições são feitas pelo painel de configuração.
* Calendário, para marcar datas importantes
* Tela exclusiva para alunos: carteirinha, horários e dados relevantes.
* Suporte a [Markdown](https://www.markdownguide.org/cheat-sheet/) para formatação de notícias e informações acadêmicas.

# Como Utilizar

## Pré-requisitos
Para poder rodar o aplicativo, certifique-se de ter as seguintes ferramentas disponíveis:
* [Android Studio (Panda 2 | 2025.3.2 ou mais novo)](https://developer.android.com/studio?hl=pt-br)
* Um projeto no [Google Firebase](https://firebase.google.com/?hl=pt-br)

## Setup
1. Clone o repositório em sua máquina:
```bash
git clone https://github.com/VitorMCR/dux-scholar.git
```
2. Adquira uma chave API de graça do Gemini em https://aistudio.google.com/api-keys?hl=pt-br
3. Abra o projeto no Android Studio e crie um arquivo `app.properties` no escopo do projeto. Adicione dentro deste arquivo a chave API recém-adquirida:
```
API_KEY="SUA_CHAVE_AQUI"
```
4. Crie um **novo projeto do Firebase**, exclusivo para este app.
5. No Android Studio, vá em Arquivo > Ferramentas > Firebase e ative as seguintes opções. Siga os passos, vinculando ao projeto recém-criado:
  * *Authentication > ... using a custom authentication system*
  * *Realtime Database > Get started with Realtime Database*
6. No *Dashboard* do seu projeto do Firebase, procure e inicie ***Authentication*** e ***Realtime Database***.
7. Na página ***Authentication***, crie um usuário qualquer - este será sua conta de **administrador**.
8. No Android Studio, [configure um emulador](https://developer.android.com/studio/run/emulator?hl=pt-br) ou [conecte seu celular via "depuração USB"](https://developer.android.com/studio/run/device?hl=pt-br) e inicie o app.
9. Faça log-in com a conta que você criou no *Dashboard*. Acesse o **ícone de lápis** no canto superior direito para abrir o painel de configuração.
10. Aproveite!

*O projeto não está completo e atualmente se encontra em hiato. Sinta-se livre para criar um fork e incrementar funcionalidades!*
