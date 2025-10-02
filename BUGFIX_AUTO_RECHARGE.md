# Bug Fix: Bateria Auto-Recarga ao Zerar

## 🐛 Problema Identificado

### Comportamento Incorreto
Quando uma bateria (Small Alkaline Battery) era drenada até **0 FE**, ela automaticamente era reinicializada com energia aleatória entre **50-70%** da capacidade.

**Logs do problema:**
```
[Dev: Drained 166500 FE (66600 J) from [Small Alkaline Battery]!]
[Dev: Drained 98636 FE (39454 J) from [Small Alkaline Battery]!]
[Dev: Drained 88492 FE (35397 J) from [Small Alkaline Battery]!]
[Dev: Drained 105248 FE (42099 J) from [Small Alkaline Battery]!]
```

Cada vez que drenada, a bateria voltava com energia aleatória diferente.

### Comportamento Esperado
A carga inicial aleatória (50-70%) deveria ocorrer **apenas quando o item é craftado**, não quando a bateria fica vazia durante o uso.

---

## 🔍 Causa Raiz

### Código Problemático (ANTES)
**Arquivo:** `RandomInitialEnergyStorage.java`

```java
private void initializeRandomEnergy() {
    if (!initialized && super.getEnergyStored() == 0) {  // ❌ PROBLEMA AQUI
        // Generate random energy between 50% and 70% of capacity
        RandomSource random = RandomSource.create();
        float randomPercentage = 0.5f + (random.nextFloat() * 0.2f);
        int initialEnergy = Math.round(getMaxEnergyStored() * randomPercentage);
        setEnergy(initialEnergy);
        initialized = true;
    }
}
```

**Problema:** A condição `super.getEnergyStored() == 0` é verdadeira em **duas situações**:
1. ✅ Item recém-craftado (nunca teve energia definida)
2. ❌ Item drenado até zero (teve energia, mas agora está vazio)

A flag `initialized` era redefinida a cada nova instância da capability, então não persistia entre usos do item.

---

## ✅ Solução Implementada

### Código Corrigido (DEPOIS)
**Arquivo:** `RandomInitialEnergyStorage.java`

```java
private void initializeRandomEnergyIfNew() {
    // Check if the component has ever been set (null means brand new item)
    if (!parent.has(energyComponent)) {  // ✅ CORREÇÃO: Verifica se DataComponent existe
        // Generate random energy between 50% and 70% of capacity
        RandomSource random = RandomSource.create();
        float randomPercentage = 0.5f + (random.nextFloat() * 0.2f);
        int initialEnergy = Math.round(getMaxEnergyStored() * randomPercentage);
        setEnergy(initialEnergy);
    }
}
```

**Solução:** Usar `parent.has(energyComponent)` para verificar se o DataComponent foi **alguma vez definido**:
- ❌ `false` (ausente) = Item **novo**, nunca teve energia → inicializar com carga aleatória
- ✅ `true` (presente) = Item **existente**, já teve energia definida → não recarregar

---

## 🔧 Mudanças Técnicas

### 1. Removido Sistema de Flag
**ANTES:**
```java
private boolean initialized = false;
```
- Flag não persiste entre instâncias da capability
- Não distingue "item novo" de "item drenado"

**DEPOIS:**
```java
private final MutableDataComponentHolder parent;
private final DataComponentType<Integer> energyComponent;
```
- Armazena referências para verificar presença do DataComponent
- Usa sistema de componentes persistentes do NeoForge

### 2. Lógica de Inicialização
**ANTES:**
```java
if (!initialized && super.getEnergyStored() == 0) {
    // Recarrega se energia == 0, independente do motivo
}
```

**DEPOIS:**
```java
if (!parent.has(energyComponent)) {
    // Só carrega se componente nunca foi definido (item novo)
}
```

### 3. Método Renomeado
- ❌ `initializeRandomEnergy()` - Nome genérico
- ✅ `initializeRandomEnergyIfNew()` - Nome explica comportamento

### 4. Remoção de Override
**ANTES:**
```java
@Override
public int getEnergyStored() {
    if (!initialized) {
        initializeRandomEnergy();  // Verifica a cada chamada
    }
    return super.getEnergyStored();
}
```

**DEPOIS:**
```java
// Sem override - inicialização só no construtor
```

---

## 🧪 Como Testar a Correção

### Teste 1: Item Novo (Deve ter carga aleatória)
```bash
# 1. Craftar nova bateria
# 2. Verificar energia
/rcenergy info
# Esperado: 50-70% (aleatório)
```

### Teste 2: Drenar e Verificar (Não deve recarregar)
```bash
# 1. Pegar bateria com carga
/rcenergy info
# Output: 83250 / 166500 FE (50%)

# 2. Drenar completamente
/rcenergy drain
# Output: Drained 83250 FE

# 3. Verificar energia novamente
/rcenergy info
# Esperado: 0 / 166500 FE (0%)
# ❌ ANTES: Voltaria para 50-70% aleatório
# ✅ AGORA: Permanece em 0 FE
```

### Teste 3: Recarregar e Drenar Novamente
```bash
# 1. Recarregar bateria
/rcenergy fill
# Output: Added 166500 FE

# 2. Drenar novamente
/rcenergy drain
# Output: Drained 166500 FE

# 3. Verificar
/rcenergy info
# Esperado: 0 / 166500 FE (permanece vazia)
```

### Teste 4: Múltiplos Itens Novos
```bash
# Craftar 5 baterias novas
# Verificar cada uma com /rcenergy info
# Esperado: Cada uma com porcentagem diferente (50-70%)
```

---

## 📊 Comparação Antes/Depois

| Situação | ANTES | DEPOIS |
|----------|-------|--------|
| Item craftado | 50-70% ✅ | 50-70% ✅ |
| Item drenado a 0 | 50-70% ❌ | 0% ✅ |
| Item recarregado e drenado | 50-70% ❌ | 0% ✅ |
| Múltiplos itens novos | Aleatório ✅ | Aleatório ✅ |

---

## 🎯 Impacto da Correção

### ✅ Benefícios
1. **Baterias se comportam realisticamente** - Ficam vazias quando drenadas
2. **Economia funciona** - Jogadores precisam recarregar baterias vazias
3. **Troca de bateria faz sentido** - Baterias vazias permanecem vazias até recarga
4. **Comando `/rcenergy drain` funciona corretamente** - Não "magicamente" recarrega

### 🎮 Impacto no Gameplay
- **Jogador agora precisa gerenciar energia** - Baterias não se auto-recarregam
- **Charge Controller tem propósito** - Único jeito de recarregar baterias vazias
- **Sistema de troca é útil** - Trocar bateria vazia por carregada é necessário
- **Realismo aumentado** - Baterias alcalinas reais não se recarregam sozinhas

---

## 📝 Notas Técnicas

### DataComponent Persistence
O sistema de `DataComponentType` do NeoForge 1.21+ persiste automaticamente:
- Quando `setEnergy()` é chamado, o componente é **marcado como presente**
- Mesmo com valor 0, o componente continua **presente** (diferente de null)
- `parent.has(energyComponent)` retorna:
  - `false` = Componente **nunca** foi definido (item novo)
  - `true` = Componente **já** foi definido (item existente, mesmo se vazio)

### Thread Safety
A verificação `parent.has()` é thread-safe pois:
- É chamada apenas no construtor (thread única)
- Não usa estado mutável compartilhado
- DataComponents são sincronizados automaticamente pelo Minecraft

### Performance
- Verificação ocorre **apenas no construtor**
- Sem overhead em `getEnergyStored()` (removido override)
- Sem estado adicional a serializar (removido `initialized`)

---

## 🔍 Warnings de Compilação

Build gerou warnings sobre `this-escape`:
```
warning: [this-escape] possible 'this' escape before subclass is fully initialized
```

**Explicação:** Chamando `getMaxEnergyStored()` no construtor antes da classe estar completamente inicializada.

**Impacto:** ⚠️ **Nenhum** - Método é da classe pai (`ComponentEnergyStorage`) que já está inicializada.

**Alternativa (se necessário):**
```java
// Passar capacity diretamente em vez de chamar getMaxEnergyStored()
int initialEnergy = Math.round(capacity * randomPercentage);
```

---

## ✅ Status

| Item | Status |
|------|--------|
| Bug identificado | ✅ Confirmado |
| Causa raiz encontrada | ✅ `super.getEnergyStored() == 0` |
| Correção implementada | ✅ Usa `parent.has(energyComponent)` |
| Build | ✅ Sucesso (com warnings) |
| Teste manual | 🟡 Requer teste in-game |

---

## 🚀 Para Testar

1. Rodar o jogo:
   ```bash
   .\gradlew.bat runClient
   ```

2. Craftar bateria nova → verificar carga aleatória inicial

3. Drenar com comando → verificar que permanece em 0:
   ```
   /rcenergy drain
   /rcenergy info
   ```

4. Confirmar que não volta a ter carga aleatória

---

**Arquivo Modificado:** `RandomInitialEnergyStorage.java`  
**Linhas Alteradas:** 11-51  
**Tipo de Mudança:** Bug Fix  
**Prioridade:** Alta (afeta mecânica central do mod)

---

*Corrigido por: GitHub Copilot*  
*Data: 02/10/2025*  
*Commit: feature/small-battery*
