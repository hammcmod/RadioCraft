package com.arrl.radiocraft.common.blockentities;

import com.arrl.radiocraft.Radiocraft;
import com.arrl.radiocraft.api.benetworks.BENetworkObject;
import com.arrl.radiocraft.api.benetworks.INetworkObjectProvider;
import com.arrl.radiocraft.common.be_networks.DataWire;
import com.arrl.radiocraft.common.be_networks.network_objects.PabxNetworkObject;
import com.arrl.radiocraft.common.blocks.PabxBlock;
import com.arrl.radiocraft.common.init.RadiocraftBlockEntities;
import com.arrl.radiocraft.common.menus.PabxMenu;
import com.arrl.radiocraft.common.radio.microwave.MicrowaveRoutes;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.LongTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * A phone PABX: every telephone needs one (maintainer, 2026-10-30). The phones it serves (touching it, or on its data
 * wire; a phone joins the nearest PABX) call each other by extension, numbered from {@link #getFirstExtension()}.
 * <p>
 * A PABX is reached from outside by its {@linkplain #getMaster() master number}. Exchanges on a microwave link (the
 * PABX touching a microwave radio, or on its data wire) call each other by master number then extension: from 602's
 * phones, "603100" rings extension 100 on PABX 603. A PABX set {@linkplain #isBehind() behind} hangs off
 * another PABX it is wired to, as one of its lines: its master number works like an extension there, so from outside
 * 602, then 7 (the PABX behind), then 100. A dialed number is looked for at the caller's PABX, then each one above it,
 * then (from the top) at every exchange across the microwave link. It needs power ({@link PabxNetworkObject}); with
 * none, its phones (and those behind it) have no line.
 */
public class PabxBlockEntity extends BlockEntity implements INetworkObjectProvider, MenuProvider {

	public static final int FIRST_EXTENSION = 100;
	/** Longest master number and extension, digits. */
	public static final int MAX_MASTER_DIGITS = 6, MAX_EXTENSION = 99999;

	private String master = "";
	private int firstExtension = FIRST_EXTENSION;
	private boolean behind = false;
	/** The phones it has numbered, so a number set by hand later is left alone. */
	private final Set<BlockPos> numbered = new HashSet<>();
	private boolean powered = false;

	public PabxBlockEntity(BlockPos pos, BlockState state) {
		super(RadiocraftBlockEntities.PABX.get(), pos, state);
	}

	@Override
	public void onLoad() {
		super.onLoad();
		if(level != null && !level.isClientSide())
			getNetworkObject(level, worldPosition); // Joins the power wire.
	}

	@Override
	public BENetworkObject createNetworkObject() {
		return new PabxNetworkObject(level, worldPosition);
	}

	// -------------------- SETTINGS --------------------

	/** True while the wire feeds it. Server side. */
	public boolean isPowered() {
		return powered;
	}

	/** The number other exchanges dial it by; empty for none (it can then only be called from its own phones). */
	public String getMaster() {
		return master;
	}

	public int getFirstExtension() {
		return firstExtension;
	}

	/** True if it hangs off another PABX it is wired to, rather than being an exchange of its own. */
	public boolean isBehind() {
		return behind;
	}

	/** Sets the master number (digits only, up to {@link #MAX_MASTER_DIGITS}); false if it is not one. */
	public boolean setMaster(String number) {
		if(number.length() > MAX_MASTER_DIGITS || !number.chars().allMatch(Character::isDigit))
			return false;
		master = number;
		sync();
		return true;
	}

	/** Sets the first extension and numbers every phone again from it. */
	public boolean setFirstExtension(int first) {
		if(first < 0 || first > MAX_EXTENSION)
			return false;
		firstExtension = first;
		numbered.clear();
		sync();
		return true;
	}

	public void setBehind(boolean behind) {
		this.behind = behind;
		sync();
	}

	// -------------------- THE EXCHANGE --------------------

	/** The PABX a phone at {@code pos} belongs to: the nearest one touching it or on its data wire. */
	@Nullable
	public static PabxBlockEntity of(Level level, BlockPos pos) {
		List<PabxBlockEntity> found = DataWire.linked(level, pos, PabxBlockEntity.class);
		return found.isEmpty() ? null : found.get(0);
	}

	/** The telephones it serves: touching it or on its data wire, and nearer it than any other PABX. */
	public List<TelephoneBlockEntity> phones() {
		List<TelephoneBlockEntity> out = new ArrayList<>();
		if(level == null)
			return out;
		for(TelephoneBlockEntity phone : DataWire.linked(level, worldPosition, TelephoneBlockEntity.class))
			if(of(level, phone.getBlockPos()) == this)
				out.add(phone);
		return out;
	}

	/** The PABXes wired to this one (touching, or on its data wire). */
	private List<PabxBlockEntity> wired() {
		List<PabxBlockEntity> out = new ArrayList<>();
		if(level != null)
			for(PabxBlockEntity pabx : DataWire.linked(level, worldPosition, PabxBlockEntity.class))
				if(pabx != this)
					out.add(pabx);
		return out;
	}

	/**
	 * Who hangs off whom among the PABXes wired together with {@code start}: each one set behind maps to the PABX it is
	 * a line of, found outward from the exchanges (those not behind) so there are no loops. One behind with no
	 * exchange to reach is left out, and works as an exchange of its own.
	 */
	private static Map<PabxBlockEntity, PabxBlockEntity> tree(PabxBlockEntity start) {
		List<PabxBlockEntity> cluster = new ArrayList<>();
		ArrayDeque<PabxBlockEntity> queue = new ArrayDeque<>(List.of(start));
		Set<PabxBlockEntity> seen = new HashSet<>(List.of(start));
		while(!queue.isEmpty()) {
			PabxBlockEntity at = queue.poll();
			cluster.add(at);
			for(PabxBlockEntity next : at.wired())
				if(seen.add(next))
					queue.add(next);
		}
		Map<PabxBlockEntity, PabxBlockEntity> parents = new HashMap<>();
		Set<PabxBlockEntity> placed = new HashSet<>();
		for(PabxBlockEntity pabx : cluster)
			if(!pabx.behind) {
				queue.add(pabx);
				placed.add(pabx);
			}
		while(!queue.isEmpty()) {
			PabxBlockEntity at = queue.poll();
			for(PabxBlockEntity next : at.wired())
				if(next.behind && placed.add(next)) {
					parents.put(next, at);
					queue.add(next);
				}
		}
		return parents;
	}

	/** The PABX this one is a line of, or null for an exchange. */
	@Nullable
	public PabxBlockEntity parent() {
		return behind ? tree(this).get(this) : null;
	}

	/** The top of this PABX's tree, or null if it or any PABX above it has no power. */
	@Nullable
	public PabxBlockEntity exchange() {
		Map<PabxBlockEntity, PabxBlockEntity> tree = tree(this);
		PabxBlockEntity at = this;
		for(int depth = 0; at != null && depth < 32; depth++) {
			if(!at.powered)
				return null;
			PabxBlockEntity up = tree.get(at);
			if(up == null)
				return at;
			at = up;
		}
		return null;
	}

	/** The number to dial this PABX by from across the microwave link: the master numbers from the top down. */
	public String fullPrefix() {
		Map<PabxBlockEntity, PabxBlockEntity> tree = tree(this);
		StringBuilder prefix = new StringBuilder();
		PabxBlockEntity at = this;
		for(int depth = 0; at != null && depth < 32; depth++) {
			prefix.insert(0, at.master);
			at = tree.get(at);
		}
		return prefix.toString();
	}

	/** The phone {@code digits} rings at this PABX or below it: one of its extensions, or a PABX behind it by its number. */
	@Nullable
	private TelephoneBlockEntity local(String digits, Map<PabxBlockEntity, PabxBlockEntity> tree, int depth) {
		if(!powered || depth > 32)
			return null;
		for(TelephoneBlockEntity phone : phones())
			if(digits.equals(Integer.toString(phone.getExtension())))
				return phone;
		for(Map.Entry<PabxBlockEntity, PabxBlockEntity> line : tree.entrySet())
			if(line.getValue() == this) {
				PabxBlockEntity below = line.getKey();
				if(!below.master.isEmpty() && digits.startsWith(below.master) && digits.length() > below.master.length()) {
					TelephoneBlockEntity phone = below.local(digits.substring(below.master.length()), tree, depth + 1);
					if(phone != null)
						return phone;
				}
			}
		return null;
	}

	/**
	 * The phone {@code digits} rings when dialed from a phone on this PABX: at this PABX and below, then at each PABX
	 * above, then at the exchanges across the microwave link by their master numbers. Null if none.
	 */
	@Nullable
	public TelephoneBlockEntity resolve(String digits) {
		Map<PabxBlockEntity, PabxBlockEntity> tree = tree(this);
		PabxBlockEntity at = this, top = this;
		for(int depth = 0; at != null && depth < 32; depth++) {
			if(!at.powered)
				return null;
			TelephoneBlockEntity phone = at.local(digits, tree, 0);
			if(phone != null)
				return phone;
			top = at;
			at = tree.get(at);
		}
		for(PabxBlockEntity exchange : top.linkedExchanges()) {
			if(exchange.master.isEmpty() || !digits.startsWith(exchange.master) || digits.length() <= exchange.master.length())
				continue;
			TelephoneBlockEntity phone = exchange.local(digits.substring(exchange.master.length()), tree(exchange), 0);
			if(phone != null)
				return phone;
		}
		return null;
	}

	/** This exchange, then the exchanges (PABXes not behind another) reached over the microwave link from it. */
	public List<PabxBlockEntity> linkedExchanges() {
		List<PabxBlockEntity> out = new ArrayList<>(List.of(this));
		if(level == null)
			return out;
		for(MicrowaveRoutes.Route route : MicrowaveRoutes.from(level, worldPosition))
			if(route.station() instanceof PabxBlockEntity pabx && !pabx.behind && pabx.powered && !out.contains(pabx))
				out.add(pabx);
		return out;
	}

	/** True if the exchanges at the top of {@code a}'s and {@code b}'s trees can still reach each other. */
	public static boolean connected(@Nullable PabxBlockEntity a, @Nullable PabxBlockEntity b) {
		PabxBlockEntity ea = a == null ? null : a.exchange(), eb = b == null ? null : b.exchange();
		return ea != null && eb != null && (ea == eb || ea.linkedExchanges().contains(eb));
	}

	/** True if the microwave route between their exchanges has interference or a marginal signal. */
	public static boolean lossy(@Nullable PabxBlockEntity a, @Nullable PabxBlockEntity b) {
		PabxBlockEntity ea = a == null ? null : a.exchange(), eb = b == null ? null : b.exchange();
		if(ea == null || eb == null || ea == eb || ea.level == null)
			return false;
		MicrowaveRoutes.Route route = MicrowaveRoutes.to(ea.level, ea.worldPosition, eb);
		return route != null && route.lossy();
	}

	// -------------------- TICK --------------------

	public static void tick(Level level, BlockPos pos, BlockState state, BlockEntity blockEntity) {
		if(blockEntity instanceof PabxBlockEntity pabx)
			pabx.serverTick(level, state);
	}

	private void serverTick(Level level, BlockState state) {
		boolean now = getNetworkObject(level, worldPosition) instanceof PabxNetworkObject object && object.isPowered;
		if(now != powered) {
			powered = now;
			sync();
		}
		if(state.getValue(PabxBlock.LIT) != powered)
			level.setBlock(worldPosition, state.setValue(PabxBlock.LIT, powered), Block.UPDATE_CLIENTS);
		if(powered && level.getGameTime() % 20 == 0)
			number();
	}

	/** Gives each phone it has not numbered yet the lowest free extension from its first extension. */
	private void number() {
		List<TelephoneBlockEntity> phones = phones();
		for(TelephoneBlockEntity phone : phones) {
			if(numbered.contains(phone.getBlockPos()))
				continue;
			Set<Integer> taken = new HashSet<>();
			for(TelephoneBlockEntity other : phones)
				if(other != phone && numbered.contains(other.getBlockPos()))
					taken.add(other.getExtension());
			int extension = firstExtension;
			while(taken.contains(extension) && extension < MAX_EXTENSION)
				extension++;
			phone.assignExtension(extension);
			numbered.add(phone.getBlockPos().immutable());
			setChanged();
		}
		// Forget phones that are gone, so their numbers free up.
		Set<BlockPos> present = new HashSet<>();
		for(TelephoneBlockEntity phone : phones)
			present.add(phone.getBlockPos());
		if(numbered.retainAll(present))
			setChanged();
	}

	private void sync() {
		setChanged();
		if(level != null && !level.isClientSide)
			level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), Block.UPDATE_CLIENTS);
	}

	// -------------------- SAVE AND SYNC --------------------

	@Override
	protected void saveAdditional(@NotNull CompoundTag tag, HolderLookup.@NotNull Provider registries) {
		super.saveAdditional(tag, registries);
		tag.putString("master", master);
		tag.putInt("firstExtension", firstExtension);
		tag.putBoolean("behind", behind);
		ListTag list = new ListTag();
		for(BlockPos pos : numbered)
			list.add(LongTag.valueOf(pos.asLong()));
		tag.put("numbered", list);
	}

	@Override
	protected void loadAdditional(@NotNull CompoundTag tag, HolderLookup.@NotNull Provider registries) {
		super.loadAdditional(tag, registries);
		master = tag.getString("master");
		firstExtension = tag.contains("firstExtension") ? tag.getInt("firstExtension") : FIRST_EXTENSION;
		behind = tag.getBoolean("behind");
		numbered.clear();
		for(Tag entry : tag.getList("numbered", Tag.TAG_LONG))
			numbered.add(BlockPos.of(((LongTag)entry).getAsLong()));
		if(tag.contains("powered"))
			powered = tag.getBoolean("powered"); // The client's copy.
	}

	@Override
	public @NotNull CompoundTag getUpdateTag(@NotNull HolderLookup.Provider registries) {
		CompoundTag tag = saveWithoutMetadata(registries);
		tag.putBoolean("powered", powered);
		tag.putString("prefix", level == null || level.isClientSide ? "" : fullPrefix());
		return tag;
	}

	@Override
	public ClientboundBlockEntityDataPacket getUpdatePacket() {
		return ClientboundBlockEntityDataPacket.create(this);
	}

	@Override
	public void onDataPacket(net.minecraft.network.Connection net, ClientboundBlockEntityDataPacket packet, HolderLookup.Provider registries) {
		super.onDataPacket(net, packet, registries);
		prefix = packet.getTag().getString("prefix");
	}

	@Override
	public void handleUpdateTag(@NotNull CompoundTag tag, HolderLookup.@NotNull Provider registries) {
		super.handleUpdateTag(tag, registries);
		prefix = tag.getString("prefix");
	}

	/** The client's copy of {@link #fullPrefix()}, for the screen. */
	private String prefix = "";

	public String getPrefix() {
		return prefix;
	}

	// -------------------- MENU --------------------

	@Override
	public @NotNull Component getDisplayName() {
		return Component.translatable(Radiocraft.translationKey("container", "pabx"));
	}

	@Override
	public @Nullable AbstractContainerMenu createMenu(int id, @NotNull Inventory inventory, @NotNull Player player) {
		return new PabxMenu(id, this);
	}
}
