# Correções de GUI - Texturas e Botões

## ✅ Alterações Implementadas:

### 1️⃣ **Tamanhos de GUI Corrigidos**

Ajustei os tamanhos das GUIs de **176x166** (padrão Minecraft) para **256x166** para exibir as texturas completas:

- **DigitalInterfaceScreen**: `256x166` ✓
- **AntennaTunerScreen**: `256x166` ✓  
- **VHFRepeaterScreen**: `256x166` ✓
- **DuplexerScreen**: `176x166` (mantido - confirme se textura é menor)

### 2️⃣ **Digital Interface - Substituídos Botões do Minecraft**

**Antes:**
- Usava `Button.builder()` do Minecraft (botões cinzas acima da GUI)

**Depois:**
- Usa `ImageButton` da textura (igual VHF Receiver e HF 80m)
- Botões posicionados dentro da GUI: `leftPos + X, topPos + Y`

---

## ⚠️ **IMPORTANTE: Ajustes Necessários**

### 📍 **Digital Interface - Coordenadas dos Botões de Tab**

Os botões estão usando **coordenadas placeholder** que você precisa ajustar:

**Arquivo:** `DigitalInterfaceScreen.java` (linhas ~48-51)

```java
// TODO: Adjust u, v coordinates based on actual button sprites in texture
addRenderableWidget(new ImageButton(leftPos + 8, topPos + 4, 50, 16, 0, 200, WIDGETS_TEXTURE, 256, 256, (btn) -> selectTab(TAB_ARPS)));
addRenderableWidget(new ImageButton(leftPos + 60, topPos + 4, 50, 16, 50, 200, WIDGETS_TEXTURE, 256, 256, (btn) -> selectTab(TAB_MSG)));
addRenderableWidget(new ImageButton(leftPos + 112, topPos + 4, 50, 16, 100, 200, WIDGETS_TEXTURE, 256, 256, (btn) -> selectTab(TAB_RTTY)));
addRenderableWidget(new ImageButton(leftPos + 164, topPos + 4, 50, 16, 150, 200, WIDGETS_TEXTURE, 256, 256, (btn) -> selectTab(TAB_FILES)));
```

**O que ajustar:**

1. **Posição na GUI** (leftPos + X, topPos + Y):
   - `leftPos + 8` = 8 pixels da borda esquerda
   - `topPos + 4` = 4 pixels do topo
   - Ajuste conforme localização real na textura

2. **Tamanho dos botões** (largura, altura):
   - `50, 16` = 50 pixels largura x 16 altura
   - Meça os botões reais na textura

3. **Coordenadas de textura (u, v)**:
   - **u, v** = onde o sprite do botão está na textura PNG
   - Exemplos:
     - `0, 200` = canto superior esquerdo em y=200
     - `50, 200` = 50 pixels à direita, y=200
   - **Meça onde os sprites dos botões estão em `digital_interface_*.png`**

---

## 📏 **Como Encontrar as Coordenadas Corretas:**

### Método 1: Abrir PNG em Editor

1. Abra `digital_interface_rtty.png` (ou outra) em editor de imagem
2. Posicione o cursor sobre o sprite do botão
3. Anote as coordenadas X, Y do canto superior esquerdo
4. Use esses valores como **u, v** no código

### Método 2: Medição Visual

Se os botões de tab fazem parte da textura principal:
- Veja onde eles aparecem na imagem
- Exemplo: se estão no topo em y=0, use `u=X, v=0`
- Se estão em uma área separada (como widgets), use aquela coordenada

---

## 🎨 **Estrutura Atual dos Botões:**

```
ImageButton(
    leftPos + X,    // Posição horizontal na tela (onde desenhar)
    topPos + Y,     // Posição vertical na tela (onde desenhar)  
    width,          // Largura do botão em pixels
    height,         // Altura do botão em pixels
    u,              // Coordenada X na textura PNG (onde está o sprite)
    v,              // Coordenada Y na textura PNG (onde está o sprite)
    texture,        // Arquivo PNG
    256, 256,       // Tamanho total da textura
    callback        // O que fazer ao clicar
)
```

---

## 🔧 **Próximos Passos:**

### Passo 1: Confirmar Tamanhos das Texturas

Abra cada PNG e verifique tamanho real:
- Se não for 256x256, ajuste `imageWidth` no código
- Texturas menores (como 176x166) devem manter tamanho menor

### Passo 2: Ajustar Coordenadas dos Botões de Tab

No Digital Interface:
1. Localize os sprites dos botões ARPS, MSG, RTTY, FILES na textura
2. Meça posição (u, v) de cada um
3. Atualize no código (linhas 48-51 de `DigitalInterfaceScreen.java`)

### Passo 3: Teste In-Game

```bash
./gradlew runClient
```

- Verifique se texturas aparecem completas
- Verifique se botões estão nos lugares certos
- Ajuste conforme necessário

---

## 📝 **Exemplo de Ajuste:**

Se você descobrir que os botões de tab estão assim na textura:

- **ARPS**: u=10, v=250
- **MSG**: u=70, v=250
- **RTTY**: u=130, v=250
- **FILES**: u=190, v=250

Atualize o código para:

```java
addRenderableWidget(new ImageButton(leftPos + 8, topPos + 4, 50, 16, 10, 250, WIDGETS_TEXTURE, 256, 256, (btn) -> selectTab(TAB_ARPS)));
addRenderableWidget(new ImageButton(leftPos + 60, topPos + 4, 50, 16, 70, 250, WIDGETS_TEXTURE, 256, 256, (btn) -> selectTab(TAB_MSG)));
addRenderableWidget(new ImageButton(leftPos + 112, topPos + 4, 50, 16, 130, 250, WIDGETS_TEXTURE, 256, 256, (btn) -> selectTab(TAB_RTTY)));
addRenderableWidget(new ImageButton(leftPos + 164, topPos + 4, 50, 16, 190, 250, WIDGETS_TEXTURE, 256, 256, (btn) -> selectTab(TAB_FILES)));
```

---

## ✅ **Checklist Final:**

- [x] Tamanhos das GUIs ajustados para 256x166
- [x] Botões do Minecraft removidos do Digital Interface
- [x] ImageButtons adicionados com coordenadas placeholder
- [ ] **VOCÊ:** Ajustar coordenadas (u, v) dos botões de tab
- [ ] **VOCÊ:** Confirmar tamanhos reais das texturas
- [ ] **VOCÊ:** Testar in-game e ajustar posições se necessário

---

**Depois de ajustar as coordenadas, o Digital Interface terá botões de tab integrados na textura, assim como os outros rádios!**
