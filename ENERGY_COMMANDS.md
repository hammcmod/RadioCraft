# RadioCraft Energy Commands

## Overview
Comandos administrativos para gerenciar energia em itens do RadioCraft (baterias e rádios).

---

## Comandos Disponíveis

### `/rcenergy drain`
**Descrição:** Drena toda a energia do item na mão principal do jogador.

**Permissão:** Requer nível de operador 2 (OP)

**Uso:**
```
/rcenergy drain
```

**Exemplos de Saída:**
- ✅ Sucesso: `Drained 166500 FE (66600 J) from VHF Handheld Radio!`
- ❌ Sem item: `You must be holding an item in your main hand!`
- ❌ Item sem energia: `This item cannot store energy!`
- ❌ Já vazio: `This item is already empty!`

**Use Cases:**
- Testar comportamento de rádio sem bateria
- Simular bateria descarregada rapidamente
- Debug de consumo de energia

---

### `/rcenergy fill`
**Descrição:** Preenche o item na mão principal com energia máxima.

**Permissão:** Requer nível de operador 2 (OP)

**Uso:**
```
/rcenergy fill
```

**Exemplos de Saída:**
- ✅ Sucesso: `Added 166500 FE (66600 J) to Small Alkaline Battery!`
- ❌ Sem item: `You must be holding an item in your main hand!`
- ❌ Item sem energia: `This item cannot store energy!`
- ❌ Já cheio: `This item is already fully charged!`

**Use Cases:**
- Carregar baterias instantaneamente para testes
- Evitar esperar pelo Charge Controller
- Debug de funcionalidades que requerem energia

---

### `/rcenergy info`
**Descrição:** Mostra informações detalhadas sobre a energia do item na mão principal.

**Permissão:** Requer nível de operador 2 (OP)

**Uso:**
```
/rcenergy info
```

**Exemplo de Saída:**
```
=== Energy Info ===
Item: VHF Handheld Radio (VHF Handheld Radio)
Energy: 83250 / 166500 FE
Joules: 33300 / 66600 J
Watt-hours: 9.25 / 18.50 Wh
Charge: 50.0%
```

**Campos Exibidos:**
- **Item:** Nome do item e tipo detectado
- **Energy:** Energia armazenada/máxima em Forge Energy (FE)
- **Joules:** Conversão para Joules (2.5 FE = 1 J)
- **Watt-hours:** Conversão para Watt-hora (3600 J = 1 Wh)
- **Charge:** Porcentagem de carga (colorida: verde > amarelo > laranja > vermelho)

**Use Cases:**
- Verificar carga exata de bateria
- Debug de cálculos de energia
- Monitorar consumo durante testes

---

## Itens Suportados

### ✅ Itens com Armazenamento de Energia:
- **Small Alkaline Battery** (`radiocraft:small_battery`)
  - Capacidade: 166,500 FE (66,600 J / 18.5 Wh)
  - Uso: Fonte de energia para VHF Handheld e QRP Radios

- **VHF Handheld Radio** (`radiocraft:vhf_handheld`)
  - Capacidade: 166,500 FE (66,600 J / 18.5 Wh)
  - Uso: Rádio portátil com bateria interna

### ❌ Itens Sem Suporte:
- Large Battery (usa sistema de energia diferente)
- HF Radios (conectados à rede de energia)
- Outros blocos/itens do mod

---

## Conversões de Energia

O RadioCraft usa múltiplas unidades para representar energia:

| Unidade | Conversão | Exemplo |
|---------|-----------|---------|
| **Forge Energy (FE)** | Base | 166,500 FE |
| **Joules (J)** | 2.5 FE = 1 J | 66,600 J |
| **Watt-hours (Wh)** | 3600 J = 1 Wh | 18.5 Wh |

**Exemplo de bateria completa:**
- 166,500 FE
- 66,600 J
- 18.5 Wh

---

## Códigos de Cor

O comando `/rcenergy info` usa cores para indicar o nível de carga:

| Porcentagem | Cor | Indicação |
|-------------|-----|-----------|
| > 75% | 🟢 Verde | Bateria cheia |
| 50-75% | 🟡 Amarelo | Bateria boa |
| 25-50% | 🟠 Laranja | Bateria baixa |
| < 25% | 🔴 Vermelho | Bateria crítica |

---

## Exemplos de Uso

### Cenário 1: Testar Swap de Bateria
```bash
# 1. Pegar VHF Handheld na mão
# 2. Drenar energia
/rcenergy drain

# 3. Pegar bateria carregada
# 4. Trocar energias (Shift + Use)
# 5. Verificar resultado
/rcenergy info
```

### Cenário 2: Testar Consumo de Energia
```bash
# 1. Pegar VHF Handheld na mão
# 2. Carregar completamente
/rcenergy fill

# 3. Verificar energia inicial
/rcenergy info

# 4. Usar o rádio (transmitir por 1 minuto)
# 5. Verificar consumo
/rcenergy info

# Calcular consumo por minuto:
# (Energia Inicial - Energia Final) / minutos
```

### Cenário 3: Carregar Múltiplas Baterias
```bash
# Para cada bateria no inventário:
# 1. Colocar na mão
# 2. Carregar
/rcenergy fill

# 3. Guardar no inventário
# 4. Repetir
```

---

## Notas Técnicas

### Permissões
- Todos os comandos requerem **nível de operador 2** (OP level 2)
- Comando base: `/rcenergy` (Radio Craft Energy)
- Em single-player: Requer cheats habilitados

### Implementação
- Usa `IEnergyStorage` capability do NeoForge
- Suporta qualquer item com `Capabilities.EnergyStorage.ITEM`
- Thread-safe (executa no server thread)

### Limitações
- Só funciona com itens na **mão principal** (main hand)
- Não afeta energia em blocos (use comandos específicos se necessário)
- Não sincroniza instantaneamente no cliente (pode levar 1 tick)

---

## Troubleshooting

### "You must be holding an item in your main hand!"
**Causa:** Mão principal está vazia ou item está na offhand.
**Solução:** Colocar o item na mão principal (slot selecionado na hotbar).

### "This item cannot store energy!"
**Causa:** Item não possui capability de armazenamento de energia.
**Solução:** Usar apenas Small Battery ou VHF Handheld.

### Comando não aparece na lista de tab-completion
**Causa:** Jogador não tem permissão de OP.
**Solução:** Conceder OP level 2: `/op <jogador>`

### Energia não atualiza visualmente
**Causa:** Cliente ainda não recebeu atualização.
**Solução:** Aguardar 1 tick ou re-abrir inventário.

---

## Changelog

### v1.0.0 (02/10/2025)
- ✅ Implementado comando `/rcenergy drain`
- ✅ Implementado comando `/rcenergy fill`
- ✅ Implementado comando `/rcenergy info`
- ✅ Suporte para Small Battery e VHF Handheld
- ✅ Conversões automáticas FE → J → Wh
- ✅ Mensagens coloridas e formatadas

---

## Código de Referência

**Localização:** `src/main/java/com/arrl/radiocraft/common/commands/EnergyCommands.java`

**Registro:** `src/main/java/com/arrl/radiocraft/common/init/RadiocraftCommands.java`

**Capability Usado:** `net.neoforged.neoforge.capabilities.Capabilities.EnergyStorage.ITEM`

---

**Status:** ✅ Implementado e funcional  
**Build:** ✅ Sucesso  
**Testes:** 🟡 Aguardando testes in-game  

---

*Implementado por: GitHub Copilot*  
*Data: 02/10/2025*
