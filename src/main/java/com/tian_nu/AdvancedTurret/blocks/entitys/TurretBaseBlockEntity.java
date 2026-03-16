package com.tian_nu.AdvancedTurret.blocks.entitys;

import com.tian_nu.AdvancedTurret.Config;
import com.tian_nu.AdvancedTurret.blocks.ModBlocks;
import com.tian_nu.AdvancedTurret.blocks.entitys.MachineGunTurretBlockEntity;
import com.tian_nu.AdvancedTurret.blocks.entitys.MissileTurretBlockEntity;
import com.tian_nu.AdvancedTurret.items.ModItems;
import com.tian_nu.AdvancedTurret.items.SmartChipItem;
import com.tian_nu.AdvancedTurret.gui.TurretMenu;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.Connection;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.energy.IEnergyStorage;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.items.ItemStackHandler;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import net.minecraft.world.inventory.ContainerData;
import java.util.Map;
import java.util.HashMap;
import java.util.Iterator;

/**
 * 闂佺粯鍔﹂崰鏍€佸▎鎾虫槬闁告繂瀚伴悰鎾绘煛閸屾粍鍤€婵炵》绱曢埀顒€婀遍崑妯肩礊?
 * 
 * <p>缂備胶濯寸槐鏇㈠箖婵犲洦鍊锋い鏃傗拡閺佸﹪鏌ｉ妸銉ヮ伂闁稿繑锕㈤弻宀冪疀閹捐埖鎲奸梺绋胯閸斿﹪鍩€椤戣儻鍏屽褍娼″畷顐ｆ媴閸濄儲鐨戦柣搴㈢⊕閿氶柟铚傚嵆閹筹絽顭ㄩ崘銊ь槹婵炲瓨鍤庨崐鎾惰姳?/p>
 * 
 * @author tian_nu
 */
public class TurretBaseBlockEntity extends BlockEntity implements MenuProvider {
    
    // ========== ContainerData 闁诲骸婀遍崑鐔肩嵁?==========
    
    protected final ContainerData data = new ContainerData() {
        @Override
        public int get(int index) {
            return switch (index) {
                case 0 -> TurretBaseBlockEntity.this.energyStorage.getEnergyStored();
                case 1 -> TurretBaseBlockEntity.this.getMaxEnergyStored();
                default -> 0;
            };
        }

        @Override
        public void set(int index, int value) {
            // 闁诲骸绠嶉崹娲春濞戞氨鍗氭い鏍亹閸嬫挸顫濋鍌氱厬婵炴垶鎸哥粔鐟般€掗崜浣瑰暫濞达絿鐡旈崯搴ｇ磽閸愭儳鏋﹂柛蹇旓耿閺屽矁绠涢妷褏顦繛杈剧到濡銆呰瀵顭ㄩ崟顑箑霉閻樺弶鎹ｇ紓宥咁槺缁辨帒顭ㄩ崘銊ф▉闂佸憡鑹鹃張顒勵敆閻愬搫鐭楁い鏍ㄧ箓閸樻挳姊婚崶锝呬壕闁?
            // 闁哄鏅滈悷鈺呭闯闁垮鈻旈柡鍕禋濞诧綁鏌ｉ～顒€濡虹紒銊ф値enu闂佸憡鑹鹃張顒勵敆?
        }

        @Override
        public int getCount() {
            return 2;
        }
    };

    // ========== 闁汇埄鍨遍幃鍌炲闯閾忓厜鍋撶憴鍕叝缂?==========
    
    /** 闂佸憡鑹剧€氼噣鎮洪幋鐘垫／鐟滃繘宕戦弽銊х畽闁哄啫娲ら崢鎾⒑閹绘帞绠版い鏃€鍔欓弻?(T1..T5) */
    public static final int[] MAX_ENERGIES = {10000, 40000, 100000, 250000, 500000};
    /** 闂佸搫鐗冮崑鎾愁熆閸棗妫楅崢鎾⒑閹绘帞绠ｇ紒杈煐濞煎繘骞橀悜鈺佷壕闁绘柨鎼懞?*/
    public static final int MAX_TRANSFER_RATE = 1000;
    
    /** 閻庢鍠氶、濠傜暤閸屾锝夊箣閹烘梻孝闂佽桨妞掗崡鎶藉闯濞差亝鏅柛顐ｇ箓瀵法绱掑☉姗嗘Ш濡ょ姴娲︾粙澶愬焵椤掑嫭鍤婇弶鍫濆⒔缁€?*/
    public static final int AMMO_SLOTS = 9;
    /** 闂佸搫鐗冮崑鎾愁熆閸棗鍟～鐘绘煠鐎圭姴袚鐟滄澘鍟粋鎺楁嚋閺佸矈鍋呴幏鍛吋閸℃ɑ顔嶉梻浣瑰絻妤犲繒妲愬?/T5闂佽　鍋撴い鏍ㄧ☉閻︻噣鏌涘▎蹇旀拱閾﹀酣鏌?*/
    public static final int MAX_PLUGIN_SLOTS = 2;
    /** 濠殿噯绲界换瀣煂濠婂牊顥堥柕蹇嬪灪閻ｉ亶鏌￠崼姘壕婵犮垹鐖㈤崘銊э紦缂備胶瀚忛崟鎴亝閹峰懎顓奸崱妯活啀闂備焦褰冩蹇曟濞嗘劗鈻旈柣鎴炆戝鎾绘煛?T5 婵☆偅婢樼€氼噣寮抽埀顒勬煥?*/
    public static final int MAX_UPGRADE_SLOTS_PER_FACE = 3;
    
    // ========== 闂佺粯鍔﹂崰鏍€佸▎鎴犲暗閻犲洩灏欓埀顒傚厴瀵顫濋銏╂喘 ==========
    
    public enum TurretType {
        NONE,
        MACHINE_GUN,
        RAILGUN
    }
    
    // ========== 闁诲孩绋掗〃鍡涱敊?==========
    
    // ========== 闂佸湱绮敮濠傗枎閵忋倖鐓€鐎广儱娲ㄩ弸?==========
    
    // 婵炶揪缍€濞夋洟寮妶鍡樺鐎广儱妫楃拹鐔兼煟椤旇崵顦﹂柣掳鍔戝畷鎺楀Ω閵夛箑鍓婚梺娲绘娇閸斿骸鈻撻幋锔筋棃?(0-63)闂佹寧绋戦惌鍌滃垝椤栨粍濯奸柕鍫濇噹瀵法鈧鍠掗崑?
    // 闁哄鏅滈悷銈夋煂濠婂嫮鈹嶆繝闈涚墛濞堝矂鏌涢敂鍝勫闁绘柡鍋撻柟鐓庢捶閸屾粎鎲梺鎸庣☉閼活垰煤濠婂嫮鈻旈柣鎴炆戠瑧闂佺硶鏅涢幖顐ｎ殽閸ヮ剚鍎嶉柛鏇ㄥ幐閳ь剚鐗楃粋鎺旀崉閾忓湱锛涢梺?
    // 婵犵鈧啿鈧綊鎮樻径鎰仺闁靛绠戦悡鏇㈡偨椤栨碍婀版繝鈧鍕氦婵炲棗娴烽惁宥夋⒒閸涱厾绠茬憸鏉垮暞缁傛帡宕滄担鍥ｆ櫊瀹曟繈鈥﹂幒鏃傤槷闂佸憡甯楅悷銉ц姳閼碱剛鐭撻柡鍕箰濞堥箖鏌熺紒妯虹瑐婵炲棭鏁禕T
    // 闂佸搫绉烽～澶婄暤娓氣偓濡線鍩€椤掆偓鏁?闂佹寧绋掑畝鎼佸储閵堝洨纾炬い鏇楀亾婵炴挸澧庨幉鐗堟媴妞嬪海顔旈柣搴㈢⊕閿氭繝鈧鍫濈闁瑰搫绉甸浠嬫⒑閹绘帟顫﹂柍褜鍓欓崐瑙勬櫠瀹ュ棛顩烽柕澶堝妿缁犲綊姊洪幓鎺戭殭缂佲€冲暞閹棃寮崶璺烘畽闂佽　鍋撻梺顐ｇ缁€瀣归悩鎻掝劉鐟滄澘鍟粋鎺楀箚闁箑娈洪梺鍛婄懄閻楁捇鍩€?
    // 婵炶揪绲藉Λ娆徫ｉ幖浣规櫖閻忕偟鐡斿ú銈夋煛鐎ｎ偆鐭嬮柣銉ユ嚇瀵灚寰勭€ｎ亞绁锋繛瀵稿Ь椤旀劗妲愬┑鍥舵付婵☆垱顑欓崥鍥偠濞戞ê顨欓悹鎰枛瀵即顢涘▎鎴犵劶婵炴垶鏌ㄩ悧鐐垫?
    // 闂佺顑呭ú鈺咁敊閺囩偐鏌﹂柍鈺佸暞缁犳帡鏌熺紒妯虹瑐婵炲棎鍨藉顔炬媼閸︻厾顦慨鎺撶⊕椤牓顢樻繝姘闁靛鍎崇壕濠氭煏?
    
    private java.util.UUID owner;
    /** 闂佺硶鏅涢幖顐ｎ殽閸ヮ剚顥堥柕蹇嬪灪閻ｉ亶鏌涘鐓庣仯闁轰降鍊濋幃鈺呮嚋绾版ê浜惧ù锝夘棑缁夋挳鏌熺悰鈩冩珖闁绘牗绮撻弫宥呯暆閸曨厼绗￠柣鐘遍檷閸婃洟宕ｅ鑸殿棃闁靛繒濮村璺ㄢ偓娈垮枓閸嬫捇鏌?*/
    private byte enabledFacesMask = 0b111111;
    /** 闂佸綊娼ч鍛叏閳哄懏鈷旈柟閭﹀墮閻撴垿鏌￠埀顒傛嫚閹绘帗鐦栭梺鑲╁帶閸燁偄煤閸ф鏅?= 0 闁荤偞绋忛崝搴ㄥΦ濮橆厾鈻旂€广儱顦伴娆撴煕閹烘洜鍫柍?*/
    private double manualRangeLimit = 0.0D;
    /** T5 闁糕晛鎼鍥礃閸涱垳鏋傞柡鍛寸細閸忔﹢鎳為婊冾暬闁挎稑鐬煎﹢锛勨偓鍦仩婵亶鎮ч崶锔惧枠闁稿繐鐗愰々顐︽儎閺嶃儮鍋?*/
    private ItemStack builtInSmartChip = ItemStack.EMPTY;

	// ========== 闂佸憡锚椤︽娊銆侀幋锔藉殟闁稿本绮岄?- 闂佺儵鏅╅崰妤呮偉閿濆棗顕遍柕鍫濇媼閸炲搫螞閺夊灝顏悗鐟扮－閸栨牠鎳￠妶鍥х厷 ==========
	
	/** 闂佺儵鏅╅崰妤呮偉閿濆洠鍋撻崷顓炰粧缂傚秴鈹塂 -> 閻庣懓鎲￠悡锟犮€傞埡鍐／闁挎梹瀵х缓娲倵閻熸澘鏆遍柍?*/
	private final Map<Integer, Float> reservedDamage = new HashMap<>();
	/** 闂佺儵鏅╅崰妤呮偉閿濆洠鍋撻崷顓炰粧缂傚秴鈹塂 -> 婵☆偅婢樼€氼喚鈧濞婂顕€宕奸弴鐕傜吹闂佺懓顕鑼濞嗘挻鍋ㄩ柕濞垮€楅懝楣冨级閳轰焦澶勬繝鈧垾鐐解偓鎺楀川椤栨稑鈧偤鏌?*/
	private final Map<Integer, Long> reservationTime = new HashMap<>();
	/** 婵☆偅婢樼€氼喚鈧懓纾幖楣冨川椤旂瓔妲梺鍝勫暙閻栫厧螞閸ф鏅柛褏婀坈k闂佹寧绋戦¨鈧紒杈ㄧ箘閹奸箖宕ㄩ幍顔剧暫濠殿喗绺块崐鏍ь渻閸岀偞鈷掗悗娑櫳戝鎾绘煛閳ь剛鎷犻幓鎺撶槚闂佸憡甯楅悷銊╁吹濠婂牆绀夐柕濞炬櫅濞呯偤鏌￠埀顒併偅閸愮偓娈ㄧ紓?*/
	private static final long RESERVATION_TIMEOUT = 200; // 10缂?

	// ========== 闂佺厧鍘栫划娆撳闯閾忓厜鍋撳☉娅亪宕?==========
    
    private int currentTransferRate = getMaxTransferRateForTier();
    private BaseEnergyStorage energyStorage = createEnergyStorage(getMaxEnergyForTier(), currentTransferRate);
    
    private final LazyOptional<IEnergyStorage> energyCapability = LazyOptional.of(() -> energyStorage);
    
    // ========== 闂佺粯銇涢弲娑㈠箹瑜忛埀顒佺⊕閿氶柛?==========
    
    private final ItemStackHandler ammoInventory = new ItemStackHandler(AMMO_SLOTS) {
        @Override
        protected void onContentsChanged(int slot) {
            setChanged();
            syncToClient();
        }

        @Override
        public boolean isItemValid(int slot, @NotNull ItemStack stack) {
            return isValidAmmoForMountedTurrets(stack);
        }
    };
    
    // 婵炶揪缍€濞夋洟寮妶澶婂珘闁逞屽墯瀵板嫯顦抽摝搴∶归敐鍛ｉ柡鍡欏枛閺屽矁绠涢妷褏顦柣搴℃贡閸嬨倕顬婇鐐茬煑妞ゆ牗绮嶉弳蹇斾繆閸欏鐏婄紓宥呮嚇閹粙鎯囨總绨峆luginSlotCount()闂佸憡鍔曢崯鍧楁偩?
    private final ItemStackHandler basePluginSlot = new ItemStackHandler(MAX_PLUGIN_SLOTS) {
        @Override
        protected void onContentsChanged(int slot) {
            setChanged();
            checkCreativePowerComponent();
            syncToClient();
        }

        @Override
        public int getSlotLimit(int slot) {
            return 1;
        }

        @Override
        public boolean isItemValid(int slot, @NotNull ItemStack stack) {
            // 闂佸憡鐟禍婊堝储閹寸姵濯肩紒瀣闊剛绱掑☉姗嗘Ш濡ょ姴娲畷妤佹媴缁涘鏅犻梺姹囧妼鐎氼叀娼曟繛杈剧到缁夐鈧灚鐗犲畷鍫曞级閹存繃鏆ラ梺琛″亾闁诡垎鍐ㄦ辈闂佺粯銇涢弲娑㈠箹?
            if (slot >= getPluginSlotCount()) return false;
            return hasPluginSlot() && isPluginItem(stack);
        }
    };

    private final ItemStackHandler[] faceUpgradeSlots = new ItemStackHandler[] {
            createFaceUpgradeHandler(Direction.DOWN),
            createFaceUpgradeHandler(Direction.UP),
            createFaceUpgradeHandler(Direction.NORTH),
            createFaceUpgradeHandler(Direction.SOUTH),
            createFaceUpgradeHandler(Direction.WEST),
            createFaceUpgradeHandler(Direction.EAST)
    };

    private ItemStackHandler createFaceUpgradeHandler(Direction face) {
        return new ItemStackHandler(MAX_UPGRADE_SLOTS_PER_FACE) {
            @Override
            protected void onContentsChanged(int slot) {
                setChanged();
                syncToClient();
            }

            @Override
            public int getSlotLimit(int slot) {
                return 4;
            }

            @Override
            public boolean isItemValid(int slot, @NotNull ItemStack stack) {
                if (slot >= getUpgradeSlotsPerFace()) return false;
                return isUpgradeComponent(stack);
            }
        };
    }
    
    private final IItemHandler combinedInventory = new IItemHandler() {
        @Override
        public int getSlots() {
            return AMMO_SLOTS + MAX_PLUGIN_SLOTS + (6 * MAX_UPGRADE_SLOTS_PER_FACE);
        }
        
        @Override
        public @NotNull ItemStack getStackInSlot(int slot) {
            if (slot < AMMO_SLOTS) return ammoInventory.getStackInSlot(slot);
            if (slot < AMMO_SLOTS + MAX_PLUGIN_SLOTS) return basePluginSlot.getStackInSlot(slot - AMMO_SLOTS);
            int faceSlot = slot - AMMO_SLOTS - MAX_PLUGIN_SLOTS;
            int faceIndex = faceSlot / MAX_UPGRADE_SLOTS_PER_FACE;
            int upgradeIndex = faceSlot % MAX_UPGRADE_SLOTS_PER_FACE;
            if (faceIndex < 0 || faceIndex >= 6) return ItemStack.EMPTY;
            return faceUpgradeSlots[faceIndex].getStackInSlot(upgradeIndex);
        }
        
        @Override
        public @NotNull ItemStack insertItem(int slot, @NotNull ItemStack stack, boolean simulate) {
            if (slot < AMMO_SLOTS) return ammoInventory.insertItem(slot, stack, simulate);
            if (slot < AMMO_SLOTS + MAX_PLUGIN_SLOTS) return basePluginSlot.insertItem(slot - AMMO_SLOTS, stack, simulate);
            int faceSlot = slot - AMMO_SLOTS - MAX_PLUGIN_SLOTS;
            int faceIndex = faceSlot / MAX_UPGRADE_SLOTS_PER_FACE;
            int upgradeIndex = faceSlot % MAX_UPGRADE_SLOTS_PER_FACE;
            if (faceIndex < 0 || faceIndex >= 6) return stack;
            return faceUpgradeSlots[faceIndex].insertItem(upgradeIndex, stack, simulate);
        }
        
        @Override
        public @NotNull ItemStack extractItem(int slot, int amount, boolean simulate) {
            if (slot < AMMO_SLOTS) return ammoInventory.extractItem(slot, amount, simulate);
            if (slot < AMMO_SLOTS + MAX_PLUGIN_SLOTS) return basePluginSlot.extractItem(slot - AMMO_SLOTS, amount, simulate);
            int faceSlot = slot - AMMO_SLOTS - MAX_PLUGIN_SLOTS;
            int faceIndex = faceSlot / MAX_UPGRADE_SLOTS_PER_FACE;
            int upgradeIndex = faceSlot % MAX_UPGRADE_SLOTS_PER_FACE;
            if (faceIndex < 0 || faceIndex >= 6) return ItemStack.EMPTY;
            return faceUpgradeSlots[faceIndex].extractItem(upgradeIndex, amount, simulate);
        }
        
        @Override
        public int getSlotLimit(int slot) {
            if (slot < AMMO_SLOTS) return ammoInventory.getSlotLimit(slot);
            if (slot < AMMO_SLOTS + MAX_PLUGIN_SLOTS) return basePluginSlot.getSlotLimit(slot - AMMO_SLOTS);
            int faceSlot = slot - AMMO_SLOTS - MAX_PLUGIN_SLOTS;
            int faceIndex = faceSlot / MAX_UPGRADE_SLOTS_PER_FACE;
            int upgradeIndex = faceSlot % MAX_UPGRADE_SLOTS_PER_FACE;
            if (faceIndex < 0 || faceIndex >= 6) return 0;
            return faceUpgradeSlots[faceIndex].getSlotLimit(upgradeIndex);
        }
        
        @Override
        public boolean isItemValid(int slot, @NotNull ItemStack stack) {
            if (slot < AMMO_SLOTS) return ammoInventory.isItemValid(slot, stack);
            if (slot < AMMO_SLOTS + MAX_PLUGIN_SLOTS) return basePluginSlot.isItemValid(slot - AMMO_SLOTS, stack);
            int faceSlot = slot - AMMO_SLOTS - MAX_PLUGIN_SLOTS;
            int faceIndex = faceSlot / MAX_UPGRADE_SLOTS_PER_FACE;
            int upgradeIndex = faceSlot % MAX_UPGRADE_SLOTS_PER_FACE;
            if (faceIndex < 0 || faceIndex >= 6) return false;
            return faceUpgradeSlots[faceIndex].isItemValid(upgradeIndex, stack);
        }
    };
    
    private final LazyOptional<IItemHandler> itemCapability = LazyOptional.of(() -> combinedInventory);
    
    // ========== 闂佸搫顑呯€氫即鍩€椤掑倸孝闁搞倝浜跺?==========
    
    public TurretBaseBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.TURRET_BASE.get(), pos, state);
    }
    
    // ========== 闂佺娴氶崜娆撳矗閿熺姴妫橀悷娆忓閵?==========
    
    public int getTier() {
        BlockState state = getBlockState();
        if (state.is(ModBlocks.TURRET_BASE_T5.get())) return 5;
        if (state.is(ModBlocks.TURRET_BASE_T4.get())) return 4;
        if (state.is(ModBlocks.TURRET_BASE_T3.get())) return 3;
        if (state.is(ModBlocks.TURRET_BASE_T2.get())) return 2;
        if (state.is(ModBlocks.TURRET_BASE_T1.get())) return 1;
        return 1;
    }

    private int getMaxEnergyForTier() {
        int tier = getTier();
        int idx = Math.max(0, Math.min(MAX_ENERGIES.length - 1, tier - 1));
        return MAX_ENERGIES[idx];
    }

    private BaseEnergyStorage createEnergyStorage(int capacity, int transferRate) {
        return new BaseEnergyStorage(capacity, transferRate, transferRate);
    }

    private void ensureEnergyCapacity() {
        int desired = getMaxEnergyForTier();
        int transferRate = getMaxTransferRateForTier();
        if (energyStorage.getMaxEnergyStored() == desired && currentTransferRate == transferRate) return;
        int stored = energyStorage.getEnergyStored();
        BaseEnergyStorage next = createEnergyStorage(desired, transferRate);
        next.setEnergyStored(Math.min(stored, desired));
        energyStorage = next;
        currentTransferRate = transferRate;
    }

    private int getMaxTransferRateForTier() {
        return switch (getTier()) {
            case 1 -> Config.turretBaseMaxTransferRateT1 > 0 ? Config.turretBaseMaxTransferRateT1 : 100;
            case 2 -> Config.turretBaseMaxTransferRateT2 > 0 ? Config.turretBaseMaxTransferRateT2 : 200;
            case 3 -> Config.turretBaseMaxTransferRateT3 > 0 ? Config.turretBaseMaxTransferRateT3 : 600;
            case 4 -> Config.turretBaseMaxTransferRateT4 > 0 ? Config.turretBaseMaxTransferRateT4 : 4000;
            case 5 -> Config.turretBaseMaxTransferRateT5 > 0 ? Config.turretBaseMaxTransferRateT5 : 10000;
            default -> MAX_TRANSFER_RATE;
        };
    }

    public boolean hasBuiltInSmartChip() {
        return getTier() >= 5;
    }

    private ItemStack getOrCreateBuiltInSmartChip() {
        if (!hasBuiltInSmartChip()) {
            return ItemStack.EMPTY;
        }
        if (builtInSmartChip.isEmpty() || !(builtInSmartChip.getItem() instanceof SmartChipItem)) {
            builtInSmartChip = new ItemStack(ModItems.SMART_CHIP.get());
        }
        return builtInSmartChip;
    }

    public int getUpgradeSlotsPerFace() {
        return switch (getTier()) {
            case 1 -> 0;
            case 2 -> 1;
            case 3 -> 2;
            case 4 -> 2;
            case 5 -> 3;
            default -> 0;
        };
    }

    /**
     * 闂佸吋鍎抽崲鑼躲亹閸ヮ剙绠甸柟鍝勭У椤愯姤淇婇崣澶婄亰缂傚秴鎳樺顐ｅ閺夋垶顏?
     * T1: 闂佸搫鍟版慨浣冦亹閸愨晝顩烽柤鍛婃櫕?
     * T2-T3: 1婵炴垶鎼╂禍婵娿亹閸愨晝顩烽柤鍛婃櫕?
     * T4-T5: 2婵炴垶鎼╂禍婵娿亹閸愨晝顩烽柤鍛婃櫕?
     */
    public int getPluginSlotCount() {
        return switch (getTier()) {
            case 1 -> 0;
            case 2, 3 -> 1;
            case 4, 5 -> 2;  // T4/T5闂佸憡鐟ラ張顒冩綍
            default -> 0;
        };
    }

    public boolean hasPluginSlot() {
        return getTier() >= 2;
    }

    public IItemHandler getAmmoInventory() {
        return ammoInventory;
    }

    public IItemHandler getBasePluginSlot() {
        return basePluginSlot;
    }

    public IItemHandler getFaceUpgradeSlots(Direction face) {
        return faceUpgradeSlots[face.get3DDataValue()];
    }
    
    public IEnergyStorage getEnergyStorage() {
        return energyStorage;
    }
    
    /**
     * 闂佸吋鍎抽崲鑼躲亹閸ヮ亗浜归柟鎯у暱椤ゅ懘鎮楀☉娅亪宕戝澶嬪剭闁告洦鍠栭崢鎾⒑?
     */
    public int getEnergyStored() {
        return energyStorage.getEnergyStored();
    }
    
    /**
     * 闂佸吋鍎抽崲鑼躲亹閸ヮ剙瀚夐柍褜鍓氬鍕樁闁稿繑锕㈤弻宀冪疀閹捐埖鎲奸梺?
     */
    public int getMaxEnergyStored() {
        ensureEnergyCapacity();
        return energyStorage.getMaxEnergyStored();
    }
    
    public void consumeEnergy(int amount) {
        ensureEnergyCapacity();
        int extracted = energyStorage.extractEnergy(amount, false);
        if (extracted > 0) {
            setChanged();
            syncToClient();
        }
    }
    
    public ItemStack getPluginStack() {
        // 閸忓牐顕伴惇鐔风杽閼侯垳澧栭敍瀛? 濞屸剝婀侀惇鐔风杽閼侯垳澧栭弮璺哄晙閸ョ偤鈧偓閸掓澘鍞寸純顔垮П閻楀洢鈧?
        int slotCount = Math.min(getPluginSlotCount(), basePluginSlot.getSlots());
        for (int i = 0; i < slotCount; i++) {
            ItemStack stack = basePluginSlot.getStackInSlot(i);
            if (!stack.isEmpty() && stack.getItem() instanceof SmartChipItem) {
                return stack;
            }
        }
        return getOrCreateBuiltInSmartChip();
    }
    
    /**
     * 闁兼儳鍢茶ぐ鍥箥閳ь剟寮垫径瀣祷濞寸姾顔愮槐娆撴偨閵娿倗鑹惧鑸电瑜板啯绂掔捄鍝勭秾闁告梻濮存慨娑㈡嚄閺傘倗绀?
     */
    public java.util.List<ItemStack> getAllPluginStacks() {
        java.util.List<ItemStack> plugins = new java.util.ArrayList<>();
        int slotCount = Math.min(getPluginSlotCount(), basePluginSlot.getSlots());
        for (int i = 0; i < slotCount; i++) {
            ItemStack stack = basePluginSlot.getStackInSlot(i);
            if (!stack.isEmpty()) {
                plugins.add(stack);
            }
        }
        if (!hasRealSmartChip() && hasBuiltInSmartChip()) {
            plugins.add(getOrCreateBuiltInSmartChip());
        }
        return plugins;
    }

    private boolean hasRealSmartChip() {
        int slotCount = Math.min(getPluginSlotCount(), basePluginSlot.getSlots());
        for (int i = 0; i < slotCount; i++) {
            ItemStack stack = basePluginSlot.getStackInSlot(i);
            if (!stack.isEmpty() && stack.getItem() instanceof SmartChipItem) {
                return true;
            }
        }
        return false;
    }
    
    public boolean isFriendlyFire() {
        ItemStack stack = getPluginStack();
        if (!stack.isEmpty()) {
            return com.tian_nu.AdvancedTurret.items.SmartChipItem.isFriendlyFire(stack);
        }
        return false;
    }

    public boolean isPredictiveAiming() {
        ItemStack stack = getPluginStack();
        if (!stack.isEmpty()) {
            return com.tian_nu.AdvancedTurret.items.SmartChipItem.isPredictiveAiming(stack);
        }
        return false;
    }

    public byte getEnabledFacesMask() {
        return enabledFacesMask;
    }
    
    public boolean isFaceEnabled(Direction face) {
        if (!hasTurretOnFace(face)) return false;
        return (getEnabledFacesMask() & (1 << face.get3DDataValue())) != 0;
    }

    public boolean hasTurretOnFace(Direction face) {
        Level level = getLevel();
        if (level == null) return false;
        BlockPos turretPos = getBlockPos().relative(face);
        BlockEntity be = level.getBlockEntity(turretPos);
        if (be instanceof MachineGunTurretBlockEntity turret) {
            TurretBaseBlockEntity base = turret.getBaseEntity();
            return base != null && base.getBlockPos().equals(getBlockPos());
        }
        if (be instanceof RailgunTurretBlockEntity turret) {
            TurretBaseBlockEntity base = turret.getBaseEntity();
            return base != null && base.getBlockPos().equals(getBlockPos());
        }
        if (be instanceof RocketTurretBlockEntity turret) {
            TurretBaseBlockEntity base = turret.getBaseEntity();
            return base != null && base.getBlockPos().equals(getBlockPos());
        }
        if (be instanceof MissileTurretBlockEntity turret) {
            TurretBaseBlockEntity base = turret.getBaseEntity();
            return base != null && base.getBlockPos().equals(getBlockPos());
        }
        if (be instanceof LaserTurretBlockEntity turret) {
            TurretBaseBlockEntity base = turret.getBaseEntity();
            return base != null && base.getBlockPos().equals(getBlockPos());
        }
        if (be instanceof GrenadeLauncherTurretBlockEntity turret) {
            TurretBaseBlockEntity base = turret.getBaseEntity();
            return base != null && base.getBlockPos().equals(getBlockPos());
        }
        if (be instanceof JunkTurretBlockEntity turret) {
            TurretBaseBlockEntity base = turret.getBaseEntity();
            return base != null && base.getBlockPos().equals(getBlockPos());
        }
        if (be instanceof PhaseFieldTurretBlockEntity turret) {
            TurretBaseBlockEntity base = turret.getBaseEntity();
            return base != null && base.getBlockPos().equals(getBlockPos());
        }
        if (be instanceof ResonanceFieldTurretBlockEntity turret) {
            TurretBaseBlockEntity base = turret.getBaseEntity();
            return base != null && base.getBlockPos().equals(getBlockPos());
        }
        return false;
    }
    
    // Setters now update the ItemStack (Update the FIRST found chip)
    public void setFriendlyFire(boolean friendlyFire) {
        ItemStack stack = getPluginStack();
        if (!stack.isEmpty()) {
            com.tian_nu.AdvancedTurret.items.SmartChipItem.setFriendlyFire(stack, friendlyFire);
            setChanged();
        }
    }

    public void setPredictiveAiming(boolean predictiveAiming) {
        ItemStack stack = getPluginStack();
        if (!stack.isEmpty()) {
            com.tian_nu.AdvancedTurret.items.SmartChipItem.setPredictiveAiming(stack, predictiveAiming);
            setChanged();
        }
    }

    public void setEnabledFacesMask(byte enabledFacesMask) {
        this.enabledFacesMask = enabledFacesMask;
        setChanged();
        syncToClient();
    }
    
    public void setFaceEnabled(Direction face, boolean enabled) {
        byte mask = enabledFacesMask;
        if (enabled) {
            mask |= (byte) (1 << face.get3DDataValue());
        } else {
            mask &= (byte) ~(1 << face.get3DDataValue());
        }
        setEnabledFacesMask(mask);
    }
    
    public java.util.UUID getOwner() {
        return owner;
    }


    public void setOwner(java.util.UUID owner) {
        this.owner = owner;
        setChanged();
        syncToClient();
    }
    public double getManualRangeLimit() {
        return manualRangeLimit;
    }

    public void setManualRangeLimit(double manualRangeLimit) {
        this.manualRangeLimit = manualRangeLimit <= 0.0D ? 0.0D : Math.min(256.0D, manualRangeLimit);
        setChanged();
        syncToClient();
    }
    
    /**
     * 闁荤姴娲弨閬嶆儑閹殿喒鍋撻獮鍨仾闁糕晜绋撶划鈺咁敍濞戞瑧鍑介梺?
     */
    public void requestClientUpdate() {
        syncToClient();
    }
    
    // ========== 濠电偞鎸搁幉锟犲垂濞嗘挻鐒婚柡鍕箳鐢?==========
    
    public static void tick(Level level, BlockPos pos, BlockState state, TurretBaseBlockEntity blockEntity) {
        if (level.isClientSide) return;

        blockEntity.ensureEnergyCapacity();
        
        boolean changed = false;
        
        // 濠碘槅鍋€閸嬫捇鏌＄仦璇插姎闁搞劑浜堕弻鍛存偐閸濆嫬骞嬮梻浣瑰絻缁绘帞鍒掑鍡欘浄?
        if (blockEntity.hasCreativePowerComponent()) {
            int maxEnergy = blockEntity.energyStorage.getMaxEnergyStored();
            if (blockEntity.energyStorage.getEnergyStored() < maxEnergy) {
                // 闂佺儵鏅涢悺銊ф暜鐎靛憡濯奸柛鎾楀懏鐎┑鐘差煭缁辨洟寮幖浣规櫖闁割偁鍨婚幖蹇涘级閳轰焦顎檈ceiveEnergy闂佹眹鍔岄崺鎻硏Receive闂傚倸瀚崝鏇㈠春濡ゅ懏鏅?
                blockEntity.setEnergyFull();
                changed = true;
            }
        } else {
            // 婵犮垽顤傛禍顏勎ｆ總鍛婂殑闁兼亽鍎辩徊璇裁归悩鐑樼【鐟滄澘鍊块幃?
            // 闂佸搫顦埀顒€寮堕浠嬫煥濞戞瑦鐨戞繛鍛囧嫬绶?婵?闂佺粯鍔﹂崰鏍€佸▎鎰鐎广儱娲ㄩ弸鍌氣槈閹剧鏀婚柡宀€鍠栧畷锝夘敍濞嗗海闉嶉梺娲讳簻椤戝懘宕虹仦鎯х窞闁冲搫鍟犻弫?
            if (blockEntity.hasSolarPlugin()) {
                boolean isDaytime = level.getDayTime() % 24000 < 12000; // 0-12000闂佸搫瀚烽崹鍐测枍瑜庡?
                // 濠碘槅鍋€閸嬫捇鏌＄仦璇插姎闁绘柡鍋撻柟鐓庢捶閸屾粎鎲梺鍝勫€块埀顒佺〒椤忛亶鏌″鍛倯婵″弶鎮傚畷銉╂晝娴ｇ骞嬮梺娲讳簻椤戝懘宕虹仦鎯х窞闁冲搫鍟犻弫鍕煥濞戞澧曟繛鍙夌矋缁嬪宕崟顓炴暏闁圭厧娲烽崒婊呮啰闂佸搫鍊婚幊鎾广亹閺屻儲鍤勯柤鎭掑劜缁犳帡鏌ｉ幇闈涙灈妞ゃ垺鐟╅弫?
                boolean canSeeSky = level.canSeeSky(pos.above());
                if (isDaytime && canSeeSky) {
                    int generated = com.tian_nu.AdvancedTurret.Config.solarEnergyGeneration;
                    int added = blockEntity.addEnergyDirectly(generated);
                    if (added > 0) {
                        changed = true;
                    }
                }
            }
            
            // 缂備椒鍕橀崹濠氭偂閼稿灚濮滄い鎺嗗亾閻庡灚鐓￠獮鎾诲箳瀹ュ棭鍋ㄩ梺鎸庣⊕閻喚鍒掗悩宸殨缁绢厼鎳庣粊鈺備繆閼艰泛袚缂佸鍏橀幊鎾澄旈崘顭戝妷闂?缂備椒鍕橀崹濠氭偂閸洖閿ゆ俊銈呭缁侇噣鏌熺拠鈩冪窔閻犳劗鍠栭幊妤佺鐎ｎ亝顏?
            if (blockEntity.hasRedstoneConversionPlugin()) {
                int energyPerRedstone = com.tian_nu.AdvancedTurret.Config.redstoneToEnergyRatio;
                int energyPerRedstoneBlock = 18000; // 缂備椒鍕橀崹濠氭偂閸洖閿ゆ俊銈呭缁侇噣鏌涢弽銊у⒊闁稿繑锕㈤弻?
                int maxEnergy = blockEntity.energyStorage.getMaxEnergyStored();
                int currentEnergy = blockEntity.energyStorage.getEnergyStored();
                int space = maxEnergy - currentEnergy;
                
                // 婵炴潙鍚嬮敋闁告ɑ绋戦埥澶愬醇閿濆倸浜炬俊銈呭暙椤掋垽鏌ｉ鐐叉毐婵炵》绻濋弫宥夊醇閿濆懎骞嬮梻浣瑰絻缁绘垵煤閹稿骸绶炴慨妯哄⒔缁€?
                if (space >= energyPerRedstoneBlock) {
                    for (int i = 0; i < blockEntity.ammoInventory.getSlots(); i++) {
                        net.minecraft.world.item.ItemStack stack = blockEntity.ammoInventory.getStackInSlot(i);
                        if (!stack.isEmpty() && stack.getItem() == net.minecraft.world.item.Items.REDSTONE_BLOCK) {
                            stack.shrink(1);
                            blockEntity.addEnergyDirectly(energyPerRedstoneBlock);
                            changed = true;
                            break;
                        }
                    }
                }
                
                // 闂佸憡鍔曠粔鐢电矓閻戣姤鍤€婵°倕鍟銏ゆ煟椤撶偟锛嶉柣?
                if (!changed && space >= energyPerRedstone) {
                    for (int i = 0; i < blockEntity.ammoInventory.getSlots(); i++) {
                        net.minecraft.world.item.ItemStack stack = blockEntity.ammoInventory.getStackInSlot(i);
                        if (!stack.isEmpty() && stack.getItem() == net.minecraft.world.item.Items.REDSTONE) {
                            stack.shrink(1);
                            blockEntity.addEnergyDirectly(energyPerRedstone);
                            changed = true;
                            break;
                        }
                    }
                }
            }
        }
        
// 闂佺粯鍔﹂崰鏍€佸▎鎾寸劵闁哄嫬绻掔敮鍡涙煟濠婂嫭绶叉繝鈧鍫熷仺閺夊牃鏅涢顏嗙磼閺傛鍎戞繛鍫熷灴閹瑩顢欑喊杈ㄦ櫓闂佸搫鍊婚幊鎾愁焽閿涘嫧鍋撻崷顓炰粧缂傚秴顑嗗鍕礋椤撶喎鈧?
		// 闂佺硶鏅涢幖顐ｎ殽閸ヮ剙鐭楁い蹇撴川椤︿即鎮归幇鍓佺ɑ鐟滈鐒︾粭鐔封槈濠婂啫骞嬮梻?

		// 濠电偞鎸搁幊鎰板箖婵犲啯浜ら柛銉ｅ妽閸╁倿鏌ｉ妸銉ヮ仹婵犵鍋撻柣搴ゎ潐婵炲﹪銆傞埡鍐／?
		blockEntity.clearExpiredReservations(level.getGameTime());

		if (changed) {
            blockEntity.setChanged();
            blockEntity.syncToClient();
        }
    }
    
    // ========== 闂佸湱绮敮濠傗枎閵忕姈娑㈠焵椤掑嫬钃熼柕澶涢檮閻撴瑦绻?==========
    
    private boolean hasCreativePowerComponent() {
        int slotCount = Math.min(getPluginSlotCount(), basePluginSlot.getSlots());
        for (int i = 0; i < slotCount; i++) {
            ItemStack stack = basePluginSlot.getStackInSlot(i);
            if (!stack.isEmpty() && stack.is(ModItems.CREATIVE_POWER_COMPONENT.get())) {
                return true;
            }
        }
        return false;
    }
    
    /**
     * 濠碘槅鍋€閸嬫捇鏌＄仦璇插姕婵″弶鎮傚畷銉╂晜閼恒儳鐣虫繝銏ゎ杺娴滎亜危婵傚憡鍤勯柤鎭掑劚缁茶霉?
     */
    public boolean hasSolarPlugin() {
        int slotCount = Math.min(getPluginSlotCount(), basePluginSlot.getSlots());
        for (int i = 0; i < slotCount; i++) {
            ItemStack stack = basePluginSlot.getStackInSlot(i);
            if (!stack.isEmpty() && stack.is(ModItems.SOLAR_PLUGIN.get())) {
                return true;
            }
        }
        return false;
    }
    
    /**
     * 濠碘槅鍋€閸嬫捇鏌＄仦璇插姕婵″弶鎮傚畷銉╂晜閼恒儳鐣抽悗娈垮枤椤㈠﹤鐣甸崒鐐茬倞闁绘劕鐡ㄩ弳顏堟煙缂佹ê绗傛繛?
     */
    public boolean hasAmmoRecyclingPlugin() {
        int slotCount = Math.min(getPluginSlotCount(), basePluginSlot.getSlots());
        for (int i = 0; i < slotCount; i++) {
            ItemStack stack = basePluginSlot.getStackInSlot(i);
            if (!stack.isEmpty() && stack.is(ModItems.AMMO_RECYCLING_PLUGIN.get())) {
                return true;
            }
        }
        return false;
    }
    
    /**
     * 濠碘槅鍋€閸嬫捇鏌＄仦璇插姕婵″弶鎮傚畷銉╂晜閼恒儳鐣崇紓浣峰嫎閸ㄥ鎮￠懜鍨妞ゆ巻鍋撻悗鍨叀楠炴捇骞掑鍡╁仺
     */
    public boolean hasRedstoneConversionPlugin() {
        int slotCount = Math.min(getPluginSlotCount(), basePluginSlot.getSlots());
        for (int i = 0; i < slotCount; i++) {
            ItemStack stack = basePluginSlot.getStackInSlot(i);
            if (!stack.isEmpty() && stack.is(ModItems.REDSTONE_CONVERSION_PLUGIN.get())) {
                return true;
            }
        }
        return false;
    }
    
    /**
     * 濠碘槅鍋€閸嬫捇鏌＄仦璇插姕婵″弶鎮傚畷銉╂晜閼恒儳鐣抽梺娲诲枟濞兼瑥顭囬弽顓炵闁瑰搫绉甸?
     */
public boolean hasDestructionPlugin() {
		int slotCount = Math.min(getPluginSlotCount(), basePluginSlot.getSlots());
		for (int i = 0; i < slotCount; i++) {
			ItemStack stack = basePluginSlot.getStackInSlot(i);
			if (!stack.isEmpty() && stack.is(ModItems.DESTRUCTION_PLUGIN.get())) {
				return true;
			}
		}
		return false;
	}

	/**
	 * 濠碘槅鍋€閸嬫捇鏌＄仦璇插姕婵″弶鎮傚畷銉╂晝閳ь剟骞嗘惔銊﹀仺闁靛鍊楅崯濠囨煕濡警妲兼い銏″灴閹崇偤宕掑鍐惧敽濠碘槅鍨埀顒€纾涵鈧?
	 */
	public boolean isThriftyMode() {
		ItemStack stack = getPluginStack();
		if (!stack.isEmpty()) {
			return com.tian_nu.AdvancedTurret.items.SmartChipItem.isThriftyMode(stack);
		}
		return false;
	}

	// ========== 闂佸憡锚椤︽娊銆侀幋锔藉殟闁稿本绮岄?- 闂佺儵鏅╅崰妤呮偉閿濆棗顕遍柕鍫濇媼閸炲搫螞閺夊灝顏悗鐟扮－閸栨牠鎳￠妶鍥х厷闂佸搫鍊介～澶屾兜?==========

/**
	 * 婵☆偅婢樼€氼喚鈧濞婇幆鍕敊閻ｅ苯鐏辨繛瀵割儠閸婃洟顢?
	 * @param entityId 闂佺儵鏅╅崰妤呮偉閿濆洠鍋撻崷顓炰粧缂傚秴鈹塂
	 * @param damage 婵☆偅婢樼€氼垶顢橀崨濠傤嚤闁靛牆鎷嬮崬?
	 * @param currentHealth 闂佺儵鏅╅崰妤呮偉閿濆牄浜归柟鎯у暱椤ゅ懘鏌ｉ姀銏犳瀻闁圭绻濆畷?
	 * @param gameTime 閻熸粎澧楅幐鍛婃櫠閻樺眰鈧帡宕ㄦ繝鍌滀簽闂佸搫鍟悥鐓幬?
	 * @return 婵☆偅婢樼€氼喚鈧濞婂畷銉︽償閿濆棛鐛ラ梺鍝勭Т濞层劌鈻撻幋锕€绀堥柍琛″亾缂傚秵鍨垮鍨緞鐎ｎ偅鐝￠梺姹囧灮閸犲酣骞婇敓鐘茬９缁绢參顥撶粈鍕偣閹邦喖鏋戦柡鍡欏枔閹即濡歌娴犳盯鏌ｉ埡濠傛灍闁绘牭绲介蹇涙嚑妫版繃钑夐梺鍛婂灩缁垰顭囬崘顔芥櫖?
	 */
	public float reserveDamage(int entityId, float damage, float currentHealth, long gameTime) {
		// 缂備線纭搁崹瀹犫叾婵☆偅婢樼€氼喚鈧娅曠€靛ジ濡堕崪浣告暯闂佹寧绋戦惌渚€鍩€椤掆偓婵傛梻绮径鎰強妞ゆ牗绻嶅ú顒勬煟閳哄倻澹勭紒杈ㄧ懇瀵劑顢涘☉妯兼Х婵犮垼鍩栧銊╁磻閺嶃劎绠欓柡鍌涘閸婇亶鏌￠崘銊ヮ暢闁诲寒鍨跺畷娆撳幢濡や礁鈧崬鈽夐幘顖氫壕闂佺儵鏅╅崰妤呮偉閿濆鏅?
		float existing = reservedDamage.getOrDefault(entityId, 0.0f);
		float totalReserved = existing + damage;
		
		reservedDamage.put(entityId, totalReserved);
		reservationTime.put(entityId, gameTime);

		// 闁荤姳绶ょ槐鏇㈡偩缂佹﹫绱ｉ柛鏇ㄥ幖椤斿﹪鏌涘顒佹崳婵炲牊鍨垮畷婊堝煛閳ь剛绱為幋锔藉仺闁绘棃鏀遍崵鎺楁煕?
		float remainingHealth = currentHealth - totalReserved;
		return remainingHealth;
	}

	/**
	 * 闁诲繐绻戠换鍡涙儊椤栨稓鈻旈柧蹇曟嚀娴犲繐鈹戞径瀣棃妞わ絺鏅濋惀顏堟晜閻愵剛鐛ラ梺鍝勭Т濞诧絽鈹戦埀顒勬倵閸︻収鍔滅紒杈ㄧ懄娣囧﹪宕掑☉姘嚱闂佺儵鏅╅崰妤呮偉閿濆拋鍟呴柤濂割杺濞煎爼鏌涜箛鏃€鏋勭紒顕呭墴閹瑩顢欑喊杈ㄦ櫓婵☆偅婢樼€氼喚鈧濞婇弫宥囦沪閽樺浠ч柡澶嗘櫆閺屻劌煤閺夊崋lse闂?
	 * @param entityId 闂佺儵鏅╅崰妤呮偉閿濆洠鍋撻崷顓炰粧缂傚秴鈹塂
	 * @param damage 婵☆偅婢樼€氼垶顢橀崨濠傤嚤闁靛牆鎷嬮崬?
	 * @param currentHealth 闂佺儵鏅╅崰妤呮偉閿濆牄浜归柟鎯у暱椤ゅ懘鏌ｉ姀銏犳瀻闁圭绻濆畷?
	 * @param gameTime 閻熸粎澧楅幐鍛婃櫠閻樺眰鈧帡宕ㄦ繝鍌滀簽闂佸搫鍟悥鐓幬?
	 * @return 婵犵鈧啿鈧綊鎮樻径鎰闁归偊鍓欓～鐘参涢弶鍨伀閻庣娅曞濠氬棘閹稿海顦rue闂佹寧绋戦懟顖炪€呰瀵顭ㄩ崨顔剧崶闂佸搫绉村ú銈夊礄閿涘嫭鍋栨い鎰╁灩瀵版挸霉閻樺磭澧遍柛瀣墬缁诲懘寮崼顐ｆ缂備焦鎷濈粻鎴﹀吹椤曗偓瀵爼鍩€椤掍焦浜ら柡鍌涘缁€鈧琭alse
	 */
	public boolean tryReserveDamage(int entityId, float damage, float currentHealth, long gameTime) {
		// 濠碘槅鍋€閸嬫捇鏌＄仦璇插姕婵″弶鎮傚畷銉╂晝閳ь剟宕欓敓鐘插珘濠㈣埖鍔﹂弳鏇犵磼?
		float existingReservation = reservedDamage.getOrDefault(entityId, 0.0f);
		float existingRemainingHealth = currentHealth - existingReservation;
		
		// 婵犵鈧啿鈧綊鎮樻径濠庡晠闁告瑥顦扮粻鎺懳涢弶鍨伀閻庣懓纾埀顑跨祷婢瑰牓宕佃閹嫰顢欓悾灞界伇"濠殿喗绻嗛濠冾殽?闂佹寧绋戦懟顖炲垂椤栨稓鈻旂€广儱鐗嗛崢鏉懳涢弶鍨伀閻?
		if (existingRemainingHealth <= 0) {
			return false;
		}
		
		// 婵☆偅婢樼€氼喚鈧濞婇獮瀣箛椤掆偓椤?
		reservedDamage.put(entityId, damage);
		reservationTime.put(entityId, gameTime);
		return true;
	}

	/**
	 * 闂佸吋鍎抽崲鑼躲亹閸ヮ剚鍎庢い鏃傛櫕閸ㄨ偐鈧懓鎲￠悡锟犮€傞埡鍐／闁挎梻鍋撻悾鍗灻圭粭鍝勨偓鏇㈩敊?
	 * @param entityId 闂佺儵鏅╅崰妤呮偉閿濆洠鍋撻崷顓炰粧缂傚秴鈹塂
	 * @return 閻庣懓鎲￠悡锟犮€傞埡鍐／闁挎梻鍋撻悾鍗灻圭粭鍝勨偓鏇㈩敊婵犲洤纾?
	 */
	public float getReservedDamage(int entityId) {
		return reservedDamage.getOrDefault(entityId, 0.0f);
	}

	/**
	 * 闂佸憡鐟﹂悧妤冪矓闁垮顕遍柕鍫濇媼閸炲搫螞閺夊灝顏悗瑙勫▕閺佸秹宕奸姀锛勭崶闂佸搫绉村ú锝呪槈椤忓懎绶為弶鍫涘妼閻忔鎱ㄥ┑濠庡敽濡ょ姵鎮傚顕€骞嗛幍顔筋啀闂佹眹鍩勯悡澶屾?
	 * @param entityId 闂佺儵鏅╅崰妤呮偉閿濆洠鍋撻崷顓炰粧缂傚秴鈹塂
	 */
	public void cancelReservation(int entityId) {
		reservedDamage.remove(entityId);
		reservationTime.remove(entityId);
	}

	/**
	 * 缂佺虎鍙庨崰娑㈩敇缂佹ê顕遍柕鍫濇媼閸炶櫣鈧懓鎲￠悡锟犲焵椤掑倸校闁搞劍宀搁弫宥夊醇閻斿憡姣庨梺鍛婂灱椤曆囧箠閳╁啰鈻旀い鎾跺仦閸婄敻鎮圭€ｎ亜鏆熼柡浣靛€濋弫宥呯暆閸曨偅鐝甸梺琛″亾濡わ絽鍠氶弳鏇犵磼閹惧懓顓虹紒?
	 * @param entityId 闂佺儵鏅╅崰妤呮偉閿濆洠鍋撻崷顓炰粧缂傚秴鈹塂
	 * @param damage 闁诲骸婀遍崑銈咁瀶椤栫偞鐒婚柣妯哄暱閻忓洭鏌ｉ妸銉ヮ仹婵犵鍋撻柣?
	 */
	public void confirmDamage(int entityId, float damage) {
		float reserved = reservedDamage.getOrDefault(entityId, 0.0f);
		if (reserved <= damage) {
			// 婵☆偅婢樼€氼喚鈧濞婇幆鍐礋椤忓懐璐熼柣搴ｆ嚀閸熷潡宕欓敓鐘茬闁靛ň鏅涢崝銉╂⒑椤愩倕校闁搞劍宀搁弫宥囦沪閸撗屼紩闂傚倸瀚ㄩ崐婵嬨€傞埡鍐／?
			reservedDamage.remove(entityId);
			reservationTime.remove(entityId);
		} else {
			// 闂備緡鍠撻崝宀勫垂鎼淬垹顕遍柕鍫濇媼閸炴椽姊洪銈呅ｉ柛銊﹀哺閺佸秶浠﹂悙顒傚嚱闂佸搫鍊瑰妯绘櫠閹稿孩濯存繛鍡樻惄閺嗘洜绱?
			reservedDamage.put(entityId, reserved - damage);
		}
	}

	/**
	 * 濠电偞鎸搁幊鎰板箖婵犲啯浜ら柛銉ｅ妽閸╁倿鏌ｉ妸銉ヮ仹婵犵鍋撻柣搴ゎ潐婵炲﹪銆傞埡鍐／?
	 * @param currentTime 閻熸粎澧楅幐鍛婃櫠閻樺眰鈧帡宕ㄦ繝鍌滀簽闂佸搫鍟悥鐓幬?
	 */
	private void clearExpiredReservations(long currentTime) {
		Iterator<Map.Entry<Integer, Long>> iterator = reservationTime.entrySet().iterator();
		while (iterator.hasNext()) {
			Map.Entry<Integer, Long> entry = iterator.next();
			if (currentTime - entry.getValue() > RESERVATION_TIMEOUT) {
				int entityId = entry.getKey();
				iterator.remove();
				reservedDamage.remove(entityId);
			}
		}
	}

	/**
	 * 濠碘槅鍋€閸嬫捇鏌＄仦璇插姢婵炶弓鍗冲浠嬪炊閵娤€锕傛煕濮樺墽鐣遍柍褜鍓涢崢褏娆㈤柆宥呯哗閻犲洦褰冨В濠囨煥濞戞澧曢悽顖楀亾闁荤偞绋戦惌浣逛繆椤撶姷妫柨鏇炲€甸崑鎾诲及韫囨洖绔奸梺?
	 * @param entityId 闂佺儵鏅╅崰妤呮偉閿濆洠鍋撻崷顓炰粧缂傚秴鈹塂
	 * @param currentHealth 闂佺儵鏅╅崰妤呮偉閿濆牄浜归柟鎯у暱椤ゅ懘鏌ｉ姀銏犳瀻闁圭绻濆畷?
	 * @return 婵犵鈧啿鈧綊鎮樻径鎰剮妞ゆ梻鏅崹鐓幟归悩鑼ら柛鏂炲洤纾归柣鎾虫捣缁讳線鏌￠埀顒傛嫚閹绘帗鐦栭柡澶嗘櫆閺屻劌煤閺夘晿ue
	 */
	public boolean isTargetWorthAttacking(int entityId, float currentHealth) {
		if (!isThriftyMode()) {
			return true; // 闂佸搫鐗滄禍婊堝箚鎼淬劍鍋ㄩ柕濞垮劚缁旀挳鎮跺☉妯肩劮濠碘槅鍘鹃惀顏堟晜閼测晝顦繝娈垮枛椤戝洨鍒掗幘瀛樹氦闁哄倹瀵х粈鈧瑃rue
		}
		
		float reserved = getReservedDamage(entityId);
		float remainingHealth = currentHealth - reserved;
		
		// 闂佸憡鎸撮弲娆戠礊閹达附鍋ㄩ柣鏃堟敱閸ゆ帡鏌涙繝鍕付闁靛洤娲︾粋?闂佸綊娼х粔鎾焵椤掑倸甯剁紒鍫曚憾瀵劎鎷犻幓鎺撶槚
		return remainingHealth > 0;
	}

	private void checkCreativePowerComponent() {
        if (hasCreativePowerComponent() && level != null && !level.isClientSide) {
            setEnergyFull();
        }
    }
    
    /**
     * 闂佺儵鏅涢悺銊ф暜鐎靛憡濯奸柛鎾楀懏鐎┑鐘差煭缁辨洟寮幖浣规櫖闁割偁鍨洪弳蹇撁瑰鍐€楅柛銊╀憾閺屽懘鎮╅崫鍕箣闂備焦褰冪换鎺斿垝瀹ュ棛顩烽悹浣告贡缁€?
     */
    private void setEnergyFull() {
        if (energyStorage.getEnergyStored() < energyStorage.getMaxEnergyStored()) {
            energyStorage.setEnergyStored(energyStorage.getMaxEnergyStored());
            setChanged();
            syncToClient();
        }
    }
    
    /**
     * 闂佺儵鏅涢悺銊ф暜鐎涙ɑ娅犻柣鎰絻椤綁鏌ら崗鍛煓闁革絾妞介弫宥夊醇閵忋垺鎮橀柡澶嗘櫅濡剧瀽xReceive闂傚倸瀚崝鏇㈠春濡ゅ懏鏅?
     * @param amount 婵犫拃鍛粶濠殿喚鍋ら幆鍐礋椤旂厧骞嬮梻浣瑰絻缁绘劙鍩€?
     * @return 闁诲骸婀遍崑銈咁瀶椤栨稒娅犻柣鎰絻椤綁鏌ｉ妸銉ヮ伂闁稿繑锕㈤弻?
     */
    private int addEnergyDirectly(int amount) {
        int maxEnergy = energyStorage.getMaxEnergyStored();
        int currentEnergy = energyStorage.getEnergyStored();
        int space = maxEnergy - currentEnergy;
        int toAdd = Math.min(amount, space);
        if (toAdd > 0) {
            energyStorage.setEnergyStored(currentEnergy + toAdd);
            setChanged();
            syncToClient();
        }
        return toAdd;
    }
    
    // ========== 闂佺厧鐤囧Λ鍕叏韫囨洖瀵查柤濮愬€楅崺?==========
    
    @Override
    public @NotNull <T> LazyOptional<T> getCapability(@NotNull Capability<T> cap, @Nullable Direction side) {
        if (cap == ForgeCapabilities.ENERGY) {
            return energyCapability.cast();
        }
        if (cap == ForgeCapabilities.ITEM_HANDLER) {
            return itemCapability.cast();
        }
        return super.getCapability(cap, side);
    }
    
    @Override
    public void invalidateCaps() {
        super.invalidateCaps();
        energyCapability.invalidate();
        itemCapability.invalidate();
    }

    private final class BaseEnergyStorage extends net.minecraftforge.energy.EnergyStorage {
        private BaseEnergyStorage(int capacity, int maxReceive, int maxExtract) {
            super(capacity, maxReceive, maxExtract);
        }

        private void setEnergyStored(int energy) {
            this.energy = Math.max(0, Math.min(energy, getMaxEnergyStored()));
        }

        @Override
        public int receiveEnergy(int maxReceive, boolean simulate) {
            int received = super.receiveEnergy(maxReceive, simulate);
            if (received > 0 && !simulate) {
                setChanged();
                syncToClient();
            }
            return received;
        }

        @Override
        public int extractEnergy(int maxExtract, boolean simulate) {
            int extracted = super.extractEnergy(maxExtract, simulate);
            if (extracted > 0 && !simulate) {
                setChanged();
                syncToClient();
            }
            return extracted;
        }
    }
    
    // ========== 闂佽桨鑳舵晶妤€鐣垫笟鈧獮鎰媴妞嬪海鏆梺?==========
    
    @Override
    protected void saveAdditional(CompoundTag tag) {
        super.saveAdditional(tag);
        
        tag.put("Energy", energyStorage.serializeNBT());
        tag.put("AmmoInventory", ammoInventory.serializeNBT());
        tag.put("BasePluginSlot", basePluginSlot.serializeNBT());

        CompoundTag faces = new CompoundTag();
        for (Direction face : new Direction[] {Direction.DOWN, Direction.UP, Direction.NORTH, Direction.SOUTH, Direction.WEST, Direction.EAST}) {
            faces.put(face.getName(), ((ItemStackHandler) getFaceUpgradeSlots(face)).serializeNBT());
        }
        tag.put("FaceUpgradeSlots", faces);
        
        // 闂佸湱绮敮濠傗枎閵忋倖鐓€鐎广儱娲ㄩ弸鍌炴煟濠婂嫭绶叉繝鈧鍡忓亾濞戞顏堝磻瀹ュ鎹?PluginSlot 闂佹眹鍔岀€氼喗鏅堕崸妤€浼?NBT 婵炴垶鎼╅崣蹇曟濠靛洨鈻旂€广儱顦版禒姗€鎮烽弴姘鳖槮鐎规洜鍠栭幃顏堫敄鐠恒劎顔旈柣?
        
        if (owner != null) {
            tag.putUUID("Owner", owner);
        }
        tag.putByte("EnabledFacesMask", enabledFacesMask);
        tag.putDouble("ManualRangeLimit", manualRangeLimit);
        if (hasBuiltInSmartChip() && !builtInSmartChip.isEmpty()) {
            tag.put("BuiltInSmartChip", builtInSmartChip.save(new CompoundTag()));
        }
    }
    
    @Override
    public void load(CompoundTag tag) {
        super.load(tag);

        ensureEnergyCapacity();
        
        if (tag.contains("Energy")) {
            energyStorage.deserializeNBT(tag.get("Energy"));
        }
        
        if (tag.contains("AmmoInventory")) {
            ammoInventory.deserializeNBT(tag.getCompound("AmmoInventory"));
        } else if (tag.contains("StorageInventory")) {
            ammoInventory.deserializeNBT(tag.getCompound("StorageInventory"));
        }

        if (tag.contains("BasePluginSlot")) {
            basePluginSlot.deserializeNBT(tag.getCompound("BasePluginSlot"));
        } else if (tag.contains("PluginSlot")) {
            ItemStackHandler legacy = new ItemStackHandler(3);
            legacy.deserializeNBT(tag.getCompound("PluginSlot"));
            if (basePluginSlot.getStackInSlot(0).isEmpty()) {
                for (int i = 0; i < legacy.getSlots(); i++) {
                    ItemStack s = legacy.getStackInSlot(i);
                    if (!s.isEmpty()) {
                        basePluginSlot.setStackInSlot(0, s);
                        break;
                    }
                }
            }
        }

        if (tag.contains("FaceUpgradeSlots")) {
            CompoundTag faces = tag.getCompound("FaceUpgradeSlots");
            for (Direction face : new Direction[] {Direction.DOWN, Direction.UP, Direction.NORTH, Direction.SOUTH, Direction.WEST, Direction.EAST}) {
                if (faces.contains(face.getName())) {
                    ((ItemStackHandler) getFaceUpgradeSlots(face)).deserializeNBT(faces.getCompound(face.getName()));
                }
            }
        } else if (tag.contains("UpgradeSlots")) {
            ItemStackHandler legacy = new ItemStackHandler(2);
            legacy.deserializeNBT(tag.getCompound("UpgradeSlots"));
            ItemStackHandler down = (ItemStackHandler) getFaceUpgradeSlots(Direction.DOWN);
            for (int i = 0; i < Math.min(legacy.getSlots(), down.getSlots()); i++) {
                if (down.getStackInSlot(i).isEmpty() && !legacy.getStackInSlot(i).isEmpty()) {
                    down.setStackInSlot(i, legacy.getStackInSlot(i));
                }
            }
        }
        if (tag.contains("Owner")) {
            owner = tag.getUUID("Owner");
        }
        if (tag.contains("EnabledFacesMask")) {
            enabledFacesMask = tag.getByte("EnabledFacesMask");
        } else {
            enabledFacesMask = 0b111111;
        }
        if (tag.contains("ManualRangeLimit")) {
            manualRangeLimit = tag.getDouble("ManualRangeLimit");
        } else {
            manualRangeLimit = 0.0D;
        }
        if (tag.contains("BuiltInSmartChip")) {
            builtInSmartChip = ItemStack.of(tag.getCompound("BuiltInSmartChip"));
        } else if (hasBuiltInSmartChip()) {
            builtInSmartChip = new ItemStack(ModItems.SMART_CHIP.get());
        } else {
            builtInSmartChip = ItemStack.EMPTY;
        }
        currentTransferRate = getMaxTransferRateForTier();
    }

    private boolean isUpgradeComponent(ItemStack stack) {
        return stack.is(ModItems.ATTACK_BOOST_COMPONENT.get())
                || stack.is(ModItems.ENERGY_EFFICIENCY_COMPONENT.get())
                || stack.is(ModItems.RANGE_COMPONENT.get())
                || stack.is(ModItems.ACCURACY_COMPONENT.get())
                || stack.is(ModItems.FIRE_RATE_COMPONENT.get());
    }

    private boolean isPluginItem(ItemStack stack) {
        return stack.is(ModItems.CREATIVE_POWER_COMPONENT.get())
                || stack.is(ModItems.SMART_CHIP.get())
                || stack.is(ModItems.SOLAR_PLUGIN.get())
                || stack.is(ModItems.AMMO_RECYCLING_PLUGIN.get())
                || stack.is(ModItems.REDSTONE_CONVERSION_PLUGIN.get())
                || stack.is(ModItems.DESTRUCTION_PLUGIN.get());
    }

    private boolean isValidAmmoForMountedTurrets(ItemStack stack) {
        if (stack.isEmpty()) return false;

        Level level = getLevel();
        if (level == null) {
            return isGenericAmmo(stack);
        }

        boolean hasJunkTurret = false;
        boolean hasSpecificAmmoMatch = false;
        boolean hasMountedTurret = false;

        for (Direction face : Direction.values()) {
            BlockEntity be = level.getBlockEntity(getBlockPos().relative(face));

            if (be instanceof MachineGunTurretBlockEntity turret && turret.getBaseEntity() == this) {
                hasMountedTurret = true;
                hasSpecificAmmoMatch |= stack.is(ModItems.MACHINE_GUN_BULLET.get());
            } else if (be instanceof RailgunTurretBlockEntity turret && turret.getBaseEntity() == this) {
                hasMountedTurret = true;
                hasSpecificAmmoMatch |= stack.is(ModItems.RAILGUN_BULLET.get());
            } else if (be instanceof RocketTurretBlockEntity turret && turret.getBaseEntity() == this) {
                hasMountedTurret = true;
                hasSpecificAmmoMatch |= stack.is(ModItems.ROCKET.get());
            } else if (be instanceof MissileTurretBlockEntity turret && turret.getBaseEntity() == this) {
                hasMountedTurret = true;
                hasSpecificAmmoMatch |= stack.is(ModItems.MISSILE.get());
            } else if (be instanceof GrenadeLauncherTurretBlockEntity turret && turret.getBaseEntity() == this) {
                hasMountedTurret = true;
                hasSpecificAmmoMatch |= stack.is(ModItems.GRENADE.get());
            } else if (be instanceof JunkTurretBlockEntity turret && turret.getBaseEntity() == this) {
                hasMountedTurret = true;
                hasJunkTurret = true;
            } else if (be instanceof LaserTurretBlockEntity turret && turret.getBaseEntity() == this) {
                hasMountedTurret = true;
            } else if (be instanceof PhaseFieldTurretBlockEntity turret && turret.getBaseEntity() == this) {
                hasMountedTurret = true;
            } else if (be instanceof ResonanceFieldTurretBlockEntity turret && turret.getBaseEntity() == this) {
                hasMountedTurret = true;
            }
        }

        if (hasJunkTurret) return true;
        if (hasMountedTurret) return hasSpecificAmmoMatch;

        return isGenericAmmo(stack);
    }

    private boolean isGenericAmmo(ItemStack stack) {
        return stack.is(ModItems.MACHINE_GUN_BULLET.get())
                || stack.is(ModItems.RAILGUN_BULLET.get())
                || stack.is(ModItems.ROCKET.get())
                || stack.is(ModItems.MISSILE.get())
                || stack.is(ModItems.GRENADE.get());
    }

    public float getDamageForFace(Direction face, float baseDamage) {
        int count = countUpgradeItems(face, ModItems.ATTACK_BOOST_COMPONENT.get());
        double multiplier = Math.min(3.0, 1.0 + (count * 0.10));
        return (float) (baseDamage * multiplier);
    }

    public double getSearchRadiusForFace(Direction face, double baseRadius) {
        int count = countUpgradeItems(face, ModItems.RANGE_COMPONENT.get());
        // 濠殿噯绲界换瀣煂濠婂牊鍤戦柛鎰╁妽缁绢垳绱撴担绋款仹婵炲棎鍨洪弲鍫曟倷閹绘帩娼?闂佸搫绉堕妴瀣濠靛洨鈻斿┑鐘叉处椤庢瑥鈽夐幘铏儓闁绘柡鍋撶紒缁㈠幐閸嬫捇鏌ら悡搴℃殭婵?32闂?
        double upgradedRadius = Math.min(baseRadius + 32.0, baseRadius + count * 8);
        if (manualRangeLimit > 0.0D) {
            return Math.max(1.0D, Math.min(upgradedRadius, manualRangeLimit));
        }
        return upgradedRadius;
    }

    public int getFireRateForFace(Direction face, int baseFireRate) {
        int count = countUpgradeItems(face, ModItems.FIRE_RATE_COMPONENT.get());
        double factor = Math.max(0.20, 1.0 - (count * 0.05));
        return Math.max(1, (int) Math.round(baseFireRate * factor));
    }

    public int getEnergyCostForFace(Direction face, int baseEnergyCost) {
        int count = countUpgradeItems(face, ModItems.ENERGY_EFFICIENCY_COMPONENT.get());
        double factor = Math.max(0.20, 1.0 - (count * 0.05));
        return Math.max(1, (int) Math.ceil(baseEnergyCost * factor));
    }

    public int getUpgradeItemCountForFace(Direction face, net.minecraft.world.item.Item item) {
        return countUpgradeItems(face, item);
    }

    private int countUpgradeItems(Direction face, net.minecraft.world.item.Item item) {
        ItemStackHandler handler = (ItemStackHandler) getFaceUpgradeSlots(face);
        int total = 0;
        for (int i = 0; i < handler.getSlots(); i++) {
            ItemStack stack = handler.getStackInSlot(i);
            if (!stack.isEmpty() && stack.is(item)) {
                total += stack.getCount();
            }
        }
        return total;
    }
    
    // ========== 缂傚倸鍟崹鍦垝閸洖瑙﹂悘鐐佃檸閸?==========
    
    @Override
    public CompoundTag getUpdateTag() {
        CompoundTag tag = new CompoundTag();
        saveAdditional(tag);
        return tag;
    }
    
    @Override
    public void handleUpdateTag(CompoundTag tag) {
        load(tag);
    }
    
    @Nullable
    @Override
    public Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }
    
    @Override
    public void onDataPacket(Connection net, ClientboundBlockEntityDataPacket pkt) {
        CompoundTag tag = pkt.getTag();
        if (tag != null) {
            handleUpdateTag(tag);
        }
    }
    
    /**
     * 闂佸憡鑹鹃張顒勵敆閻愬搫鏋侀柣妤€鐗嗙粊锕傛煕閹烘挻绶叉い鎾崇秺楠炲顦版惔顔荤磽
     */
    public void syncToClient() {
        if (level != null && !level.isClientSide) {
            level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
        }
    }
    
    // ========== MenuProvider ==========
    
    @Override
    public Component getDisplayName() {
        return Component.translatable("container.turret_base");
    }
    
    @Nullable
    @Override
    public AbstractContainerMenu createMenu(int containerId, Inventory playerInventory, Player player) {
        return new TurretMenu(containerId, playerInventory, this, this.data);
    }
}
