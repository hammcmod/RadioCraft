# RadioCraft GUI Implementation - Trabalho Pendente

**Data:** 31 de Outubro de 2025  
**Issue de Referência:** https://github.com/hammcmod/RadioCraft/issues/41

## ⏳ Tarefas Pendentes (5/13)

As seguintes tarefas requerem informações adicionais, testes in-game, ou criação de assets gráficos para serem completadas.

---

### 1. VHF Receiver - Botões de Memória e Background ❌

**Descrição do Problema:**
- Botões de memória estão "quebrados" (não especificado como)
- Textura de background está "incompleta"

**Arquivo Afetado:**
- `VHFReceiverScreen.java` (localizado em `client/screens/radios/`)

**Análise Atual:**
```java
public class VHFReceiverScreen extends VHFRadioScreen<VHFReceiverMenu> {
    // imageWidth = 251, imageHeight = 106
    // Usa: vhf_receiver.png e vhf_receiver_widgets.png
    
    // Widgets atuais:
    // - Power button
    // - Frequency dial
    // - Gain dial
}
```

**Informações Necessárias:**
- [ ] Quais botões de memória devem existir? (quantidade, posição, função)
- [ ] Como os botões estão "quebrados"? (não aparecem, não funcionam, visuais incorretos?)
- [ ] Qual parte do background está incompleta? (textura cortada, áreas faltando?)
- [ ] Existe um design de referência ou mockup?

**Assets Necessários:**
- Verificar se `vhf_receiver.png` precisa ser atualizado
- Verificar se `vhf_receiver_widgets.png` possui sprites dos botões de memória
- Possivelmente criar novos sprites para botões de memória

**Ações Recomendadas:**
1. Testar in-game para ver estado atual
2. Comparar com design esperado
3. Identificar coordenadas de textura necessárias
4. Implementar `ImageButton` ou `ToggleButton` para cada botão de memória
5. Adicionar lógica de callback para armazenar/recuperar frequências

**Exemplo de Implementação (se necessário):**
```java
// Botões de memória (exemplo - posições a confirmar)
addRenderableWidget(new ImageButton(leftPos + X1, topPos + Y1, W, H, 
    u, v, widgetsTexture, 256, 256, this::onMemoryButton1));
addRenderableWidget(new ImageButton(leftPos + X2, topPos + Y2, W, H, 
    u, v, widgetsTexture, 256, 256, this::onMemoryButton2));
// ... repetir para cada botão de memória

protected void onMemoryButton1(Button button) {
    // Lógica para armazenar/carregar frequência
}
```

---

### 2. Ponteiros/Needles para Dials e Medidores ❌

**Descrição do Problema:**
- Faltam texturas de "ponteiro" em mostradores (dials) e medidores (gauges)
- Não está claro quais telas estão afetadas

**Widget Disponível:**
```java
// MeterNeedleIndicator.java existe no projeto
// Tipos: METER_HORIZONTAL, METER_VERTICAL, METER_ROTATION
// Possui física de animação para movimento suave
```

**Análise Necessária:**
1. **Revisar TODAS as telas de rádio:**
   - HFRadio10mScreen
   - HFRadio20mScreen
   - HFRadio40mScreen
   - HFRadio80mScreen
   - HFRadioAllBandScreen
   - HFReceiverScreen
   - QRPRadio20mScreen
   - QRPRadio40mScreen
   - VHFBaseStationScreen
   - VHFReceiverScreen
   - VHFHandheldScreen (se existir)

2. **Para cada tela, verificar:**
   - [ ] Quais `Dial` widgets existem?
   - [ ] Algum deles deveria ter ponteiro visual?
   - [ ] Existem medidores (power meter, SWR meter, signal strength)?
   - [ ] `MeterNeedleIndicator` está sendo usado?

**Assets Necessários:**
- Sprites de ponteiros para dials (pequenos, provavelmente 5-10 pixels)
- Sprites de needles para medidores (podem ser maiores, 20-40 pixels)
- Devem ser adicionados aos arquivos `*_widgets.png` de cada rádio

**Exemplo de Localização de Problema:**
```java
// Exemplo: HFRadio10mScreen tem dial de frequência
addRenderableWidget(new Dial(leftPos + 134, topPos + 37, 42, 45, 
    102, 0, widgetsTexture, 256, 256, 
    this::onFrequencyDialUp, this::onFrequencyDialDown));
// ↑ Este dial pode precisar de um ponteiro visual
```

**Ações Recomendadas:**
1. Abrir cada textura `*_widgets.png` em editor de imagem
2. Verificar se existem sprites de ponteiros não utilizados
3. Se não existirem, criar sprites apropriados
4. Adicionar coordenadas de textura ao código
5. Instanciar `MeterNeedleIndicator` onde necessário

**Template de Implementação:**
```java
// Para adicionar needle a um medidor
addRenderableWidget(new MeterNeedleIndicator(
    leftPos + x, topPos + y, width, height,
    needleU, needleV, needleWidth, needleHeight,
    widgetsTexture, 256, 256,
    MeterNeedleIndicator.MeterType.METER_ROTATION,
    supplierFunction, // Fornece valor 0.0-1.0
    minAngle, maxAngle // Ângulos de rotação
));
```

---

### 3. HF 80m - Reposicionar Knob Incorreto ❌

**Descrição do Problema:**
- Um "knob" (botão giratório) está posicionado incorretamente dentro da tela

**Arquivo Afetado:**
- `HFRadio80mScreen.java`

**Análise Atual:**
```java
public class HFRadio80mScreen extends HFRadioScreen<HFRadio80mMenu> {
    // imageWidth = 212, imageHeight = 211
    // Usa: hf_radio_80m.png e hf_radio_80m_widgets.png
    
    // Widgets atuais:
    // - Power button (leftPos + 10, topPos + 188)
    // - CW Button (leftPos + 90, topPos + 74)
    // - SSB Button (leftPos + 90, topPos + 54)
    // - PTT button (leftPos + 139, topPos + 163)
    // - Frequency Dial (leftPos + 42, topPos + 156)
    // - Mic Gain dial (leftPos + 122, topPos + 186)
    // - Gain dial (leftPos + 160, topPos + 186)
}
```

**Informações Necessárias:**
- [ ] Qual knob/dial específico está incorreto?
- [ ] Está sobrepondo outro elemento?
- [ ] Está posicionado fora da textura de background?
- [ ] Está na posição errada em relação ao design da textura?

**Ações Recomendadas:**
1. Abrir `hf_radio_80m.png` em editor de imagem
2. Identificar posições corretas de cada dial/knob baseado na arte
3. Testar in-game para ver qual knob está mal posicionado
4. Ajustar coordenadas `leftPos + X, topPos + Y` do widget problemático
5. Considerar remover se não deveria existir

**Possíveis Culpados:**
- Mic Gain dial: `(122, 186)` - tamanho `15x17`
- Gain dial: `(160, 186)` - tamanho `15x17`
- Frequency Dial: `(42, 156)` - tamanho `28x33`

---

### 4. Speaker/Mic - Substituir Botão por Label de Texto ❌

**Descrição do Problema:**
- "Botão" de Speaker/Mic deve ser substituído por uma etiqueta de texto estático

**Análise Atual:**
- Comentários no código mencionam "Mic gain dial" mas não há botão "Speaker/Mic" explícito
- Pode estar se referindo aos dials de ganho de microfone

**Busca Realizada:**
```java
// Grep por "Speaker" ou "Mic" encontrou apenas:
// - Comentários sobre "Mic gain dial"
// - Nenhum widget específico chamado Speaker ou Mic
```

**Informações Necessárias:**
- [ ] Qual tela específica tem este botão? (HF? VHF?)
- [ ] É um ToggleButton? ImageButton? Dial?
- [ ] Que texto deve aparecer no label? ("Speaker"? "Mic"? "Speaker/Mic"?)
- [ ] Onde deve ser posicionado o texto?
- [ ] Qual cor/estilo do texto? (branco, verde, etc.)

**Ações Recomendadas:**
1. Testar cada rádio in-game
2. Identificar qual tem botão/widget de Speaker/Mic
3. Localizar o widget no código
4. Remover o widget da `init()`
5. Adicionar renderização de texto em `renderBg()` ou `renderAdditionalBackground()`

**Template de Implementação:**
```java
@Override
protected void renderBg(@NotNull GuiGraphics guiGraphics, float partialTick, 
                        int mouseX, int mouseY) {
    super.renderBg(guiGraphics, partialTick, mouseX, mouseY);
    
    // Adicionar label de texto
    if (menu.isPowered()) { // ou sempre
        guiGraphics.drawString(font, "Speaker", 
            leftPos + X, topPos + Y, 
            0xFFFFFF); // Cor branca
        guiGraphics.drawString(font, "Mic", 
            leftPos + X2, topPos + Y2, 
            0xFFFFFF);
    }
}
```

---

### 5. Reduzir Contraste do Background das Telas ❌

**Descrição do Problema:**
- Contraste do fundo das telas dificulta a leitura
- Afeta VHFBaseStationScreen, HFRadio10mScreen, HFRadio80mScreen

**Análise Técnica:**

**Método Atual de Renderização:**
```java
// Em RadioScreen.java - classe base
@Override
public void renderBackground(@NotNull GuiGraphics pGuiGraphics, ...) {
    super.renderBackground(pGuiGraphics, pMouseX, pMouseY, pPartialTick);
    RenderSystem.setShader(GameRenderer::getPositionTexShader);
    RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F); // ← RGBA completo
    RenderSystem.setShaderTexture(0, texture);
    
    int edgeSpacingX = (this.width - this.imageWidth) / 2;
    int edgeSpacingY = (this.height - this.imageHeight) / 2;
    pGuiGraphics.blit(this.texture, edgeSpacingX, edgeSpacingY, 
        0, 0, this.imageWidth, this.imageHeight);
}
```

**Opções de Solução:**

**Opção 1: Ajustar Alpha do Shader**
```java
// Reduzir contraste diminuindo intensidade
RenderSystem.setShaderColor(0.85F, 0.85F, 0.85F, 1.0F);
// Valores menores = mais escuro
```

**Opção 2: Overlay Semitransparente**
```java
// Após renderizar textura, adicionar overlay escuro
pGuiGraphics.fill(leftPos, topPos, 
    leftPos + imageWidth, topPos + imageHeight, 
    0x40000000); // Alpha de 25% preto
```

**Opção 3: Modificar Texturas PNG**
- Editar `vhf_base_station.png`, `hf_radio_10m.png`, `hf_radio_80m.png`
- Reduzir contraste/brilho em editor de imagem (GIMP, Photoshop)
- Mais trabalhoso mas resultado mais preciso

**Informações Necessárias:**
- [ ] Testar in-game para ver contraste atual
- [ ] Qual nível de redução é desejado? (leve, moderado, forte)
- [ ] Todas as áreas da tela ou apenas áreas de display?
- [ ] Preferência: modificar código ou texturas?

**Ações Recomendadas:**
1. Capturar screenshots das 3 telas afetadas
2. Testar diferentes valores de shader color
3. Se necessário, modificar texturas PNG
4. Considerar fazer ajuste global em `RadioScreen.java` ou por tela

**Implementação Recomendada (Código):**
```java
// Em cada Screen específica, sobrescrever renderBackground:
@Override
public void renderBackground(@NotNull GuiGraphics pGuiGraphics, 
                            int pMouseX, int pMouseY, float pPartialTick) {
    super.renderBackground(pGuiGraphics, pMouseX, pMouseY, pPartialTick);
    RenderSystem.setShaderColor(0.85F, 0.85F, 0.85F, 1.0F); // Ajustar valor
    // ... resto do código de renderização
}
```

---

## 📋 Checklist de Informações Necessárias

### Para Completar as Tarefas Pendentes:

- [ ] **VHF Receiver:**
  - [ ] Screenshots ou descrição do problema atual
  - [ ] Número e posição esperada dos botões de memória
  - [ ] Descrição de qual parte do background está incompleta

- [ ] **Ponteiros/Needles:**
  - [ ] Lista de quais rádios precisam de ponteiros
  - [ ] Screenshots mostrando dials/medidores sem ponteiros
  - [ ] Especificação de qual tipo de needle (horizontal/vertical/rotação)

- [ ] **HF 80m Knob:**
  - [ ] Screenshot mostrando o knob mal posicionado
  - [ ] Identificação de qual knob específico (frequência? ganho? mic gain?)
  - [ ] Posição correta desejada

- [ ] **Speaker/Mic Label:**
  - [ ] Identificação de qual rádio tem este botão
  - [ ] Screenshot do botão atual
  - [ ] Texto exato desejado no label
  - [ ] Posição desejada do texto

- [ ] **Contraste de Tela:**
  - [ ] Screenshots das 3 telas afetadas
  - [ ] Nível de redução de contraste desejado
  - [ ] Preferência de método (código vs edição de textura)

---

## 🎨 Assets Potencialmente Necessários

### Texturas que Podem Precisar de Criação/Modificação:

1. **vhf_receiver_widgets.png**
   - Sprites de botões de memória (se não existirem)
   - Tamanho estimado: 12x12 pixels cada

2. **Arquivos *_widgets.png (Vários Rádios)**
   - Sprites de ponteiros para dials: ~5-10 pixels
   - Sprites de needles para medidores: ~20-40 pixels
   - Devem combinar com estilo visual existente

3. **Background PNGs (Opcional)**
   - `vhf_base_station.png`
   - `hf_radio_10m.png`
   - `hf_radio_80m.png`
   - Se opção de ajuste por textura for escolhida

---

## 🔍 Próximos Passos Recomendados

1. **Teste In-Game Completo:**
   - Executar `./gradlew runClient`
   - Abrir cada rádio mencionado
   - Documentar problemas visuais com screenshots
   - Anotar comportamentos inesperados

2. **Análise de Assets:**
   - Abrir todas as texturas PNG em editor
   - Verificar sprites disponíveis mas não utilizados
   - Identificar assets faltantes
   - Criar lista de sprites a criar

3. **Priorização:**
   - Começar por problemas que bloqueiam funcionalidade
   - Ajustes estéticos podem ser feitos depois
   - Criar issues separadas no GitHub para cada problema

4. **Documentação de Design:**
   - Criar mockups ou wireframes se necessário
   - Documentar especificações visuais
   - Definir padrões de UI para consistência

---

## 💡 Dicas para Implementação

### Testando GUIs In-Game:
```bash
# Compilar e executar cliente de desenvolvimento
./gradlew runClient

# Comandos úteis no jogo:
/give @s radiocraft:vhf_receiver
/give @s radiocraft:hf_radio_80m
# etc.
```

### Editando Texturas:
- **Localização:** `src/main/resources/assets/radiocraft/textures/gui/`
- **Ferramentas:** GIMP (grátis), Aseprite, Photoshop
- **Formato:** PNG com transparência
- **Tamanho:** Normalmente 256x256 para widgets

### Debugging Visual:
```java
// Adicionar retângulos de debug para ver hitboxes:
@Override
protected void renderBg(...) {
    super.renderBg(...);
    // Debug: desenhar retângulo vermelho
    guiGraphics.fill(leftPos + X, topPos + Y, 
        leftPos + X + width, topPos + Y + height, 
        0x80FF0000); // Semi-transparente vermelho
}
```

---

## 📞 Contato para Esclarecimentos

Se você for continuar este trabalho, considere:

1. Criar issues individuais no GitHub para cada tarefa pendente
2. Adicionar screenshots e descrições detalhadas
3. Marcar com labels apropriadas (bug, enhancement, assets needed)
4. Vincular à issue original #41

**Issue Original:** https://github.com/hammcmod/RadioCraft/issues/41
