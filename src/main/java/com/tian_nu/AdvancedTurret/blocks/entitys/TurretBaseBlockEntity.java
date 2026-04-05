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
import net.minecraft.world.Containers;
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
 * Core block entity for the turret base.
 *
 * <p>Handles shared energy, ammo storage, plugin slots, per-face upgrades,
 * ownership, targeting preferences, and menu synchronization.</p>
 *
 * @author tian_nu
 */
public class TurretBaseBlockEntity extends BlockEntity implements MenuProvider {
    
    // ========== Mounted Turret Types ==========
    
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
            // Menu data is read-only; values are sourced from the block entity state.
        }

        @Override
        public int getCount() {
            return 2;
        }
    };

    // ========== Mounted Turret Types ==========
    
    /** Maximum energy by base tier (T1..T5). */
    public static final int[] MAX_ENERGIES = {10000, 40000, 100000, 250000, 500000};
    /** Default transfer rate before tier-specific overrides are applied. */
    public static final int MAX_TRANSFER_RATE = 1000;
    
    /** Shared ammo slots on the base. */
    public static final int AMMO_SLOTS = 9;
    /** Maximum plugin slots available on high-tier bases. */
    public static final int MAX_PLUGIN_SLOTS = 2;
    /** Maximum upgrade slots available per mounted face. */
    public static final int MAX_UPGRADE_SLOTS_PER_FACE = 3;
    
    // ========== Mounted Turret Types ==========
    
    public enum TurretType {
        NONE,
        MACHINE_GUN,
        RAILGUN
    }
    
    // ========== Base State ==========
    
    // 婵犵數濮烽弫鎼佸磻閻樿绠垫い蹇撴缁€濠囨煃瑜滈崜姘辨崲濞戞瑥绶為悗锝庡亞椤︿即鎮楀▓鍨珮闁稿锕ユ穱濠囧醇閺囩偛鑰垮┑鈽嗗灟鐠€锕傚焵椤掍焦宕岄柟顔筋殜閻涱噣宕归鐓庮潛婵犵妲呴崑鍛淬€冮崼銉ョ鐟滅増甯楅崑鍕煕韫囨艾浜归柛妯挎椤啴濡堕崱姗嗘⒖閻庤娲滈弫鏄忔闂佹寧娲栭崐褰掓偂閻旂厧绠抽柟鎯版缁€澶愭煙鐎涙绠橀柡鍡畵閺岀喖鎮滃鍡樼暥闁诡垳鍠栧娲濞戣鲸顎嗙紓浣哄У閸ㄥ灝鐣峰鍕瘈婵﹩鍘搁幏鍝勨攽閻愯泛鐨虹紒顕呭灡缁绘盯宕堕浣哄幈闂佸搫鍊稿锟狀敁濡ゅ懏鐓冪紓浣股戝畷灞绢殽閻愬樊鍎旈柡浣稿€荤划鐢碘偓锝庡亽濡?(0-63)闂傚倸鍊搁崐鐑芥倿閿旈敮鍋撶粭娑樻噽閻瑩鏌熸潏楣冩闁稿孩顨婇弻娑氫沪閸撗呯厐闂佹悶鍔嶇换鍐Φ閸曨垰鍐€闁靛ě鍛帒濠电姵顔栭崹浼村Χ閹间礁钃熼柡鍥ュ灩闁卞洦绻濋棃娑欐悙婵炲拑缍侀幃妤€鈻撻崹顔界仌濠电偛顦板ú鐔煎春閳ь剚銇勯幒宥囪窗闁哥喎绻橀弻娑㈡偐閸愬弶璇為悗?
    // 闂傚倸鍊风粈渚€骞栭位鍥敃閿曗偓閻ょ偓绻濇繝鍌滃闁稿绻濋弻鏇㈠醇濠垫劖效闂佺粯鎸搁崐鍦崲濠靛洨绡€闁稿本鍑规导鈧梻浣哥－閹虫挾绮旈悽鍨床婵犻潧顑嗛ˉ鍫熺箾閹寸偠澹樻い銈傚亾濠电姷鏁搁崑娑㈡偋閸℃稒鍎楅柛宀€鍋涢拑鐔哥箾閹存瑥鐏柡鍛矒閺屾稑鈹戦崱妤婁患濡炪値鍓欓悧鎾愁潖閾忓湱纾兼俊顖濆吹閸樻悂姊虹粙娆惧剱闁圭懓娲濠氭偄閸忕厧浜楅柟鍏肩暘閸ㄥ搫鐣靛澶嬧拺缂佸瀵у﹢鎵磼椤斿ジ鍙勬鐐差樀椤㈡棃宕奸悢鍙夊闂備礁婀遍…鍫澝归悜钘夌叀濠㈣埖鍔栭悡鐘崇箾閼奸鍤欓柣蹇曞█閹繝濡堕崱娆戠槇婵犵數濮撮崐鎼侇敂椤愶附鐓冪紓浣股戦ˉ鍫ユ煛瀹€瀣М妤犵偛顑夐幃娆撳幢濡櫣浠奸梺鑽ゅ暀閸ャ劉鎷绘繛杈剧秬濞咃綁濡存繝鍥ㄧ厸闁告侗鍠氬ú瀛橆殽閻愬澧垫い銏℃礋閺佹劖鎯旈姀鈺佹櫖闂傚倷鐒﹂幃鍫曞磿濞差亜绀堟慨妯挎硾绾捐法鈧箍鍎遍ˇ浼存偂閺囥垺鐓涢柛銉ｅ劚婵″吋顨ラ悙鏉戝闁哄苯绉烽¨渚€鏌涢幘鏉戝摵闁诡啫鍏炬棃宕橀鍡欏姽闂備礁婀遍崕銈咁潖閻熸壆鏆﹀鑸靛姈閻擄綁鐓崶銊﹀鞍閻犳劧绻濋弻銊モ槈濞嗘垶鍒涘┑?
    // 婵犵數濮烽弫鍛婃叏閻戝鈧倹绂掔€ｎ亞鍔﹀銈嗗坊閸嬫捇鏌涢悢閿嬪仴闁糕斁鍋撳銈嗗坊閸嬫挾绱撳鍜冭含妤犵偛鍟灒閻犲洩灏欑粣鐐烘⒑瑜版帒浜伴柛妯哄悑缁傛帒煤椤忓應鎷洪梻鍌氱墛娓氭鎮￠鐘电＜闁绘ê鍟块悘鎾煙椤曞棛绡€鐎殿喗鎸虫慨鈧柍銉ュ帠缂傛挻淇婇悙顏勨偓鏍偋濡ゅ啫鏋堢€广儱鎷戦懓鍧楁煟濡も偓閻楀繒寮ч埀顒勬⒑闁偛鑻晶顔姐亜椤撴粌濮傜€规洜鍠栭、鏍箰鎼粹剝鏁繝鐢靛Х閺佹悂宕戝☉妯滄稑螖閳ь剟婀侀梺缁樺灱婵倝宕曟繝鍕ㄥ亾楠炲灝鍔氭い锔垮嵆閸╂盯骞掑Δ浣哄幈濠电偞鍨靛畷顒勭嵁濡偐纾奸柣妯烘惈閻ㄦ椽鏌熼崣澶嬪€愮€殿喖鐖煎畷褰掝敊閼恒儺鍞圭紓鍌氬€风粈渚€宕愰崷顓熸殰闁炽儱纾弳锔界節婵犲倸顏┑顖涙尦閺屾盯濡烽婊吷戝┑鐐叉噹閿曪箓鍩€椤掑喚娼愭繛鍙夘焽閹广垽宕奸妷銉т紜闂佹寧娲栭崐鍝ョ不閹惰姤鐓涢柛鎰╁妼娴滅偞淇婇懠顒€鍘存慨濠勭帛閹峰懘宕ㄦ繝鍌涙畼闂佹眹鍩勯崹閬嶃€冩繝鍥х畺闁绘垼妫勫敮閻熸粍鍨堕幈銊╁箮閼恒儳鍘剧紒鐐緲瀹曨剚鏅舵导瀛樼厽妞ゆ挾鍠愬畷宀勬煛瀹€瀣瘈鐎规洜鍠栭、鏇㈠Χ閸ヨ泛鏁瑰┑鐘垫暩閸嬫盯鎮ч幘鍓佷笉闁哄稁鍘奸拑鐔兼煟閺冨洦顏犵痪鎯у悑娣囧﹪顢涘┑鍥朵哗闂佽崵鍠愰崝鏇㈠煘閹达附鍊烽柛娆忣檧缁辩偤姊烘潪鎷屽厡妞も晝顕?    // 闂傚倸鍊搁崐椋庣矆娓氣偓楠炴牠顢曢妶鍥╃厠闂佺粯鍨堕弸鑽ょ礊閺嵮岀唵閻犺櫣灏ㄩ崝鐔兼煛閸℃劕鈧洟濡撮幒鎴僵闁挎繂鎳嶆竟鏇熺節绾版ɑ顫婇柛瀣€诲▎銏狀潩鐠虹儤妲梺閫炲苯澧柕鍥у楠炴帡宕卞鎯ь棜闂?闂傚倸鍊搁崐鐑芥倿閿旈敮鍋撶粭娑樻噽閻瑩鏌熼悜妯虹仼闁哄棗妫濋弻鐔兼⒒鐎靛壊妲梺绋胯閸旀垿寮婚弴銏犻唶婵犻潧娲ょ粣娑氱磼閹冣挃闁稿鎹囨俊鐢稿礋椤栨艾宓嗗┑掳鍊愰崑鎾趁瑰鍕姢闁宠鍨块幃娆戔偓娑櫭喊宥嗙節閳封偓閸愵喖寮板Δ鐘靛仜椤︾敻骞冭瀹曞ジ鎮㈡總澶嗗亾閹剧繝绻嗛柣鎰▕閸庡繑绻涚€电鍘存い銏＄懇瀵噣宕奸悢鍝勫箺闂佺懓鍚嬮悾顏堝垂婵犳艾鐓€闁哄洢鍨洪悡銉︽叏濮楀棗骞樼紒鈾€鍋撻梻渚€鈧偛鑻晶瀛樸亜閵忊剝顥堢€规洜鍘ч埞鎴﹀醇椤愩垺鏉搁梻鍌氬€烽懗鍫曞箠閹捐绠规い鎰╁€楅惌鎾绘煟閵忕姴顥忛柡浣稿暙闇夐柣妯烘▕閸庢劙鏌嶉柨瀣伌闁哄瞼鍠撶槐鎺懳熼搹璇″剮婵＄偑鍊ら崑鎾剁不閹捐钃熺€广儱鐗滃銊╂⒑閸涘﹥灏伴柣鐔叉櫅閻ｇ兘骞囬钘夋瀭闂佸憡娲﹂崜姘舵偟閻戣姤鍊垫鐐茬仢閸旀碍淇婇锝囨创闁靛棗鎳橀幃婊堟嚍閵壯冨妇濠电姰鍨煎▔娑㈡偋閸℃ü绻嗛悹鎭掑妿绾惧ジ鏌ｅΟ鍨毢缂佸鍎ら幈銊︾節閸愨斂浠㈠Δ鐘靛仦閹瑰洭鐛幒妤€绠ｆい鎾跺櫏閸炶尙绱撻崒姘偓椋庢媼閺屻儱鐒垫い鎺嗗亾闁哥喎娼″鎶芥倷閻戞鍘靛銈嗙墬缁嬫帡藟閸懇鍋撳▓鍨珮闁革綇绲介悾鐑藉礈娴ｇ懓纾梺缁樺灦椤洭寮抽鐐粹拻闁稿本鐟︾粊浼存煏閸″繐浜鹃梻浣侯焾椤戝棝骞戦崶褜娼栨繛宸憾閺佸啴鏌ㄥ┑鍡樼ォ闁诲繗浜槐鎾存媴閹绘帊澹曢梺璇插嚱缂嶅棝鍩€椤戞儳鐏╃紓宥咃躬楠炲啴鍩￠崨顓狀唽闂佸湱鍎ょ换鍌涗繆閻戣姤鐓熼柣鏂挎憸閻绻濋姀鈽呰€跨€规洦鍨堕、娑橆煥閸涱垳鍔归梻浣告贡閸庛倝銆冮崨顖滀笉婵鍩栭埛鎴炪亜閹惧崬濡块柣锝堥哺缁绘盯宕奸悢鎼炰虎濠殿喖锕ュ钘夌暦閵婏妇绡€闁告洦鍓欏▍妤呮⒒娴ｈ櫣甯涙い銊ユ楠炴垿宕堕鈧弰銉╂煃?
    // 婵犵數濮烽弫鎼佸磻閻樿绠垫い蹇撴缁躲倝鏌﹀Ο鐚寸礆婵炴垶菤閺嬪海鈧敻鍋婇崑鎾剁礊娓氣偓閻涱噣寮介妸锕€顎撻柣鐔哥懃鐎氼參鎮甸锔解拺閻犲洩灏欑粻鎶芥煕鐎ｎ剙鏋涢柟顔芥そ瀵剙螞閸偅銇濆┑陇鍩栧鍕偓锝庡墮楠炴劙姊虹拠鎻掑毐缂傚秴妫濆畷鎴﹀幢濞戞瑥鍓归柣搴秵閸犳鎮￠悢鍏肩厪濠㈣泛鐗嗛崝婊堟煕閵堝懏宸濋柍褜鍓氶鏍窗閺嶎厽鍊舵慨姗嗗墻閸ゆ鏌涢弴銊ュ箰闁稿鎹囬弫鎰償濠靛牊鏅肩紓鍌欐缁舵岸寮ㄦ潏鈺傚床婵炴垶鍩冮崑鎾绘偨濞堣法鍔搁梻澶婎儏椤啴濡堕崱妯锋嫻闂佸憡姊归〃鍛祫闂佸綊鍋婇崗姗€寮ㄦ禒瀣厱闁靛鍔岄悡鎰归悩瓒侇亪鍩為幋锕€鐓￠柛鈩冾殘娴狀厼顪冮妶鍡楃仸闁荤啿鏅涢悾鐑藉Ψ閳轰胶鍊炲銈嗗笂缁€浣圭閳轰讲鏀介柣鎰綑閻忥箓鏌ㄥ顓滀簻闁靛濡囬幊鍥煙椤旇偐绉烘鐐叉喘椤㈡瑩宕楅懖鈺€閭梺璇插椤旀牠宕伴弴銏犵闁硅泛顫曢埀顑跨閳藉螣闁垮鈧姊洪悷閭﹀殶濠殿喚鍏樺畷婵堟崉鐞涒剝鏂€闂佺粯鍔栧娆撴倶閿曞倹鐓涢悘鐐额嚙婵倿鏌熼銊ユ处閸婄兘鏌涢…鎴濅航婵?
    // 闂傚倸鍊搁崐鐑芥嚄閸洍鈧箓宕奸妷顔芥櫈闂佸憡渚楅崝璺好洪鍕幐闂佸憡绮堥悞锕傚疾閳哄懏鈷戦柛婵嗗瀹告繈鏌涚€ｎ偄濮嶇€殿喖顭烽弫鍐磼濞戞艾甯楅梻浣哥枃濡椼劎绮堟笟鈧鎶芥倷閻㈢數锛滈梺缁樏崯鍨归鈧弻鈥崇暆閳ь剟宕伴幘鑸殿潟闁圭儤鍤﹂悢鍏兼優闁革富鍘介崯鏉库攽閻樺灚鏆╅柛瀣☉铻為幖娣妼閸ㄥ倿鏌﹀Ο渚▓闁搞倖娲橀妵鍕籍閸屾矮澹曟繝娈垮灠閵堟悂寮诲☉銏℃櫆閻犲洦褰冪粻璇差渻閵堝繐鐦滈柛鐘愁殜楠炲牓濡搁埡浣侯唺闂佽鍎抽悘鍫熸叏閸パ屾富闁靛牆楠搁獮鏍煟濡や焦宕岄柕鍡曠铻ｉ柤濮愬€楅惁鍫濃攽椤旀枻渚涢柛鎾寸洴钘濈憸鏂款潖濞差亝顥堟繛鎴ｄ含閸旀悂姊虹粙鍧楊€楃痪缁㈠幗缁旂喖寮撮悢铏圭槇濠殿喗锕╅崢楣冨储?
    
    private java.util.UUID owner;
    private String ownerName = "";
    /** 闂傚倸鍊搁崐鐑芥嚄閸撲焦鍏滈柛顐ｆ礀閻ょ偓绻涢幋娆忕仼缂佺姾顫夐妵鍕箛閸撲胶校濠电偛鐗呯划娆撳蓟濞戙垹唯妞ゆ梻鍘ч～顏勵渻閵堝啫鍔氶柣妤佹崌瀵寮撮敍鍕澑闁诲函缍嗘禍鐐寸妤ｅ啯鈷戝ù鍏肩懅缁夘喖霉濠婂啰鍩ｇ€殿喛顕ч埥澶婎潨閸℃ê鍏婇梻浣虹帛閹哥霉闁垮顩锋い鏍仦閳锋垿寮堕悙鏉垮祮婵″墽鍏橀弻娑欑節閸愵亞鐤勫Δ鐘靛仜閸熸挳宕洪敓鐘插窛妞ゆ柨鍚嬮悿鍌滅磽娴ｇ鈷斿褎顨婇弫瀣箾鐎涙鐭嬮柛搴㈠▕濠€渚€姊洪幐搴ｇ畵妞わ箒妫勮灋闁瑰濮风壕鐓庮熆鐠轰警鍎愰悘蹇ョ畵閺屸€崇暆閳ь剟宕伴幘璇茬畺濞村吋娼欓悞娲煕閹般劍娅囩憸鏉挎喘濮婄粯鎷呴搹鐟扮闂佺粯顨嗛〃鍫ュ礆閹烘绠婚悹鍥у级閻庮剟鎮楅獮鍨姎闁硅櫕鍔欏鎶藉幢濞戞瑧鍘遍梺鍝勬储閸斿矂鐛弽顐ょ＜婵°倕顑囩弧鈧梺鍝勭焿缂嶄線骞冮埡鍛劶鐎广儱妫埀顒佺墵濮婅櫣绮欓崠鈩冩暰濠电偠灏欓崰搴ㄦ偩閻戣姤鏅查柛婊€鐒﹂崓鐢告⒑閻撳孩鍟炴い鏃€鐗曡灋闁告劦鍠楅埛鎴︽⒒閸喓銆掔紒鐘插暱閳规垿顢欓懞銉х▏濡炪倖娲╃徊鍧楀箲閸曨垰惟闁靛繒濮虫竟鏇炩攽閻愯尙澧曢柣蹇旂箞瀵悂骞樼紒妯煎幈闁诲函缍嗛崑鍛暦瀹€鍕厸?*/
    private byte enabledFacesMask = 0b111111;
    /** 闂傚倸鍊搁崐椋庣矆娴ｈ櫣绀婂┑鐘插亞閻掔晫鎲搁弮鍫涒偓渚€寮介鐐茬獩濡炪倖妫佸Λ鍕嚕閸ф鈷戦柛鎰级閹牓鏌熼崘鑼闁糕晜鐩顕€宕奸悢鍝勫箥闂傚倷绶￠崣蹇曠不閹寸偞娅犳い鏇楀亾闁哄矉缍侀獮姗€寮堕幋鐘辩礉闂備礁鎼惌澶屾崲濠靛鏄ラ柍褜鍓氶妵鍕箳閹存績鍋撻崷顓涘亾濮橆厼鍝洪柡宀€鍠撶槐鎺懳熼搹鍦嚃闂備椒绱梽鍕偋閻樺樊娼栨繛宸簼閸ゅ啴鏌嶆潪鎵槮濞寸姵锕㈠娲传閵夈儲鐝￠梺绋款儏鐎氼噣宕ｉ崨瀛樷拺缂佸妫楃€氬嘲鈻撻弴銏＄厸?= 0 闂傚倸鍊峰ù鍥х暦閻㈢纾婚柣鎰暩閻瑩鐓崶銊р槈缂佲偓婢舵劕绠规繛锝庡墮婵＄厧顩奸崨顓涙斀妞ゆ梹鏋绘笟娑㈡煕濮椻偓缁犳牠宕洪姀銈呯睄闁稿本顨呮禍鐐殽閻愯尙浠㈤柛鏃€纰嶉妵鍕晜閸喖绁銈冨灪閹告悂鍩㈡惔銊ョ閻庣數顭堥獮鍫ユ⒒娴ｅ憡璐￠柛搴涘€曠叅婵犲﹤鐗嗛柋鍥ㄣ亜韫囨挾澧涢柣?*/
    private double manualRangeLimit = 0.0D;
    /** T5 闂傚倸鍊烽懗鍓佹兜閸洖鐤炬繛鎴欏灩缁犺銇勯幇鍓佺У婵炲牅绮欓弻娑㈠Ψ椤旂厧顫╃紓浣插亾闁告劦鍠楅悡鏇熺箾閹存繂鑸归柣蹇ョ秮閺岋紕鈧綆浜滈埀顒€娼″濠氭晸閻樻彃绐涢柣搴ㄦ涧閹芥粎绮ｅΔ鍛拺缂佸娉曠粻鏌ユ煥閺囨ê鐏叉鐐诧躬閹瑦绗熼娑欐珫婵犵數濮撮敃銈夊窗濮樿泛姹叉い鎺戝閳锋垿鏌熺粙鎸庢崳闁宠棄顦甸弻锝夘敆閸屾艾浠橀梺鎸庢磸閸ㄤ粙寮澶婄闁靛鍎版竟鏇㈡⒑闂堚晛鐦滈柛妯哄悑缁傛帡鍩℃担鍙夋杸濡炪倖娲嶉崑鎾趁瑰鍐煟妤犵偛鍟抽ˇ鍦偓瑙勬礀閻栧ジ寮崒鐐茬畾鐟滃酣鎮樻惔銊︹拻濞达絼璀﹂悞鍓х磼缂佹ê濮嶉柟顔ㄥ洤绠涢柤鍓插厴閸嬫捇宕ㄩ幖顓熸櫇闂佹寧妫佸Λ鍕礈椤曗偓濮婃椽宕崟顓夌娀鏌涙惔顔兼珝鐎?*/
    private ItemStack builtInSmartChip = ItemStack.EMPTY;

	// ========== Mounted Turret Types ==========
	
	/** 闂傚倸鍊搁崐鐑芥嚄閸洖纾块柣銏㈩焾閻ら箖鏌嶉崫鍕櫣缂佹劖顨嗘穱濠囧Χ閸涱喖娅ら梺绋款儏椤︾敻寮婚妸銉㈡斀闁糕剝顭囬ˇ閬嶆⒑缁嬫鍎愰柟鐟版搐閻ｇ兘鎮滅粵瀣櫍闂佺粯鍔栨竟鍡涙煢閻㈢數纾介柛灞捐壘閳ь剙鎽滅划鏃堝箻椤旇偐鍘存繝?-> 闂傚倷娴囬褍霉閻戣棄绠犻柟鎹愵嚙缁犵喖鏌ㄩ悢鍝勑ｉ柛瀣€块弻銊╂偄閸濆嫅鐐烘煃瑜滈崜娆撳磹閸ф鏄ラ柨鐔哄Т缁€鍐┿亜韫囨挻瀚呯紒鎲嬬節濮婄粯鎷呴悷閭﹀殝濠殿喖锕ゅ﹢閬嶅焵椤掑倻鎳楅柛娑卞灣婢跺嫬鈹戦悙鍙夆枙濞存粍绻堝畷鎰版偨閸涘﹦鍘介梺缁樻煥閹诧紕娆㈤弻銉︾厸闁糕剝绋愰幉楣冩煛?*/
	private final Map<Integer, Float> reservedDamage = new HashMap<>();
	/** 闂傚倸鍊搁崐鐑芥嚄閸洖纾块柣銏㈩焾閻ら箖鏌嶉崫鍕櫣缂佹劖顨嗘穱濠囧Χ閸涱喖娅ら梺绋款儏椤︾敻寮婚妸銉㈡斀闁糕剝顭囬ˇ閬嶆⒑缁嬫鍎愰柟鐟版搐閻ｇ兘鎮滅粵瀣櫍闂佺粯鍔栨竟鍡涙煢閻㈢數纾介柛灞捐壘閳ь剙鎽滅划鏃堝箻椤旇偐鍘存繝?-> 婵犵數濮烽。钘壩ｉ崨鏉戠；闁告侗鍙庨悢鍡樹繆椤栨氨姣為柛瀣尭椤繈鎮℃惔銏壕闂備線鈧偛鑻晶鍙夈亜椤愩埄妲圭紒缁樼⊕缁绘繈宕掗妶鍛吙婵＄偑鍊栧ú宥夊磻閹惧灈鍋撶憴鍕闁靛牏顭堥锝夊箻椤旇棄浜為梺绋挎湰缁矂骞嗛悙鐑樷拻闁稿本鐟ㄩ崗宀勬煙閾忣偅宕屾い銏″哺椤㈡﹢濮€閳哄倹娅岄梻浣藉亹閳峰牓宕滃璁圭稏闁哄洨鍠撶粻楣冩煕閳╁叐鎴犱焊娴煎瓨鐓曢悗锝冨妼婵倿鏌＄仦鐐缂佺粯鐩畷褰掝敊閻熼澹曞┑掳鍊曢幊蹇涘吹鐏炶娇鏃堟晲閸涱厽娈紓浣哄珡閸ャ劎鍘撻柡澶屽仦婢瑰棝宕濆鍜佺唵閻犲搫鎼顏嗙磼缂佹绠為柛鈹惧亾濡炪倖甯掔€氼剟鎮橀幎鑺ョ厽闁归偊鍨嶉幒妤€纾婚柟鎹愵嚙缁犺櫕淇婇妶鍌氫壕閻庣懓鎲＄换鍐Φ閸曨垰鍐€闁靛ě鈧Σ鍫ユ⒑闁偛鑻晶浼存煕鐎ｃ劌鈧繂顕?*/
	private final Map<Integer, Long> reservationTime = new HashMap<>();
	/** 婵犵數濮烽。钘壩ｉ崨鏉戠；闁告侗鍙庨悢鍡樹繆椤栨氨姣為柛瀣尭椤繈鎮℃惔銏壕闂備線鈧偛鑻晶浼存煙閾忣偅灏靛瑙勬礋椤㈡﹢濮€閻樻鍟囧┑掳鍊х徊浠嬪疮椤愩埄鍟呮繝闈涙储娴滄粓鏌￠崘銊モ偓濠氬箺閸屾稐绻嗙€瑰壊鍠栭悘锔芥叏婵犲啯銇濈€规洦鍋婂畷鐔碱敃閿濆棭鍞查梻鍌欒兌缁垶鎮у鍫濆偍鐟滄垿鎮樼€ｎ喗鈷戠紒澶婃鐎氬嘲鈻撻弴銏＄厸闁告粈绀佹晶鎾煛鐏炲墽顬兼い锕€鐡ㄧ换婵嬪焵椤掑嫬閿ら柛褎锕㈠濠氬磼濞嗘垵濡介柣搴ｇ懗閸涱垳鐓撻梺瑙勵偧鐏炲倸濮傞柛鈹惧亾濡炪倖甯掗崐鑽ゅ婵傜绾ч柛顐ｇ☉婵¤法绱掗悩绛硅€块柡宀€鍠愮缓浠嬪传閵壯勬瘒闁诲氦顫夊ú蹇涘礉閹达妇宓佺€广儱顦介弫濠囨煕閹炬瀚惁鎺撶節閻㈤潧校妞ゆ梹鐗犲畷鏉课旈崘顏嗗闂佺鍕垫當缁炬儳缍婇弻鈥崇暤椤斿吋婀伴悗闈涚焸濮婅櫣绮欑捄銊т紘闂佺顑囬崑銈夊春濞戙垹绠虫俊銈勭閳ь剙鐏氱换娑㈠箣閻戝棔绱楅梺鐟扮摠缁诲秹宕甸弴銏＄厵闁告挆鍛闂佺粯鎸婚惄顖炲蓟瀹ュ牜妾ㄩ梺鍛婃尰閻╊垶鐛箛娑欏亹閻犲洩灏欓宀勬⒑閸︻厼鍔嬮柟鍛婂劤鍗辨慨妯垮煐閳锋帒霉閿濆懏鍟為柛鐔哄仱閹粙顢涘☉杈ㄧ杹闂佽鍠氶崗妯讳繆閹间礁鐓涘ù锝呮啞閸庮亝绻濋悽闈浶㈤柨鏇樺€濋幃褔宕卞▎鎴犵劶婵犮垼鍩栭崝鏍偂韫囨挴鏀介柣鎰版涧娴滅偓绻涢崨顓熷櫤缂佺粯鐩畷銊╊敍濮橆偅顫曢梻浣告惈閻鎹㈠┑瀣槬闁逞屽墯閵囧嫰骞掑澶嬵€栭梺绋款儏閹虫﹢寮诲☉銏犵疀妞ゆ棃妫挎竟鏇炩攽閻愯尙澧戦柛鏂挎捣濡?*/
	private static final long RESERVATION_TIMEOUT = 200; // 10缂?

	// ========== Mounted Turret Types ==========
    
    private int currentTransferRate = getMaxTransferRateForTier();
    private BaseEnergyStorage energyStorage = createEnergyStorage(getMaxEnergyForTier(), currentTransferRate);
    
    private final LazyOptional<IEnergyStorage> energyCapability = LazyOptional.of(() -> energyStorage);
    
    // ========== Mounted Turret Types ==========
    
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
    
    // 婵犵數濮烽弫鎼佸磻閻樿绠垫い蹇撴缁€濠囨煃瑜滈崜姘辨崲濞戞瑥绶為悗锝庡亞椤︿即鎮楀▓鍨珮闁稿锕ユ穱濠囨嚋闂堟稓绐炴繝鐢靛Т閸婃槒銇愭径鎰拻濞撴埃鍋撻柍褜鍓涢崑娑㈡嚐椤栨稒娅犳い鏍ㄥ閸嬫捇宕归锝囧嚒闁诲孩鍑归崹鎯版闂佽鍎兼慨銈夊箟瑜版帒绠圭紒顔炬嚀閻撴垹鎲搁悧鍫濈瑲闁哄懏鐓￠弻娑樷槈閸楃偟鈧椽鏌ㄥ┑鍡╂Ч闁绘挻鐩弻娑㈠焺閸愵亝鍣ч梺鍝勵儐閻╊垶寮婚敍鍕勃闁伙絽鐬奸崝顔剧磽娴ｅ搫校闁烩晩鍨辨穱濠囨偪椤栵絾鞋婵＄偑鍊х€靛矂宕抽敐澶婅摕闁挎繂妫欓崕鐔兼煃閳轰礁鏆㈢痪顓涘亾闂傚倷鑳堕…鍫ヮ敄閸愵喖纾归柡鍥ｆ噰閳ь剚鐗楃换婵嬪炊閼稿灚娅栭梻浣虹帛閸旀浜稿▎鎾村亗闁瑰瓨绻嶅〒濠氭煏閸繃顥犲褜鍓涚槐鎺楊敊閼测敩褏鈧鍠栭悿鍥╃不濞戙垹妫橀柛鎾楀懐鏆伴梻鍌欑閹诧繝鎮烽妷鈹у洭顢涢悙鏉戔偓璺衡攽閻樻彃顏痪鍓ф櫕閳ь剙绠嶉崕閬嶅箠韫囨稑鍚归柛銉墯閻撴瑦銇勯弮鍥у惞闁活厽鐟╅弻鐔碱敍濮橆厼顦╃紓浣芥閺咁偊鎳為崡鐐垫殝闁哥偛顎乬inSlotCount()闂傚倸鍊搁崐椋庣矆娓氣偓楠炲鏁撻悩鑼槷闂佸搫娲㈤崹褰掓偪椤曗偓閺屾稖顦虫い銊ユ瀹?
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
            // 闂傚倸鍊搁崐椋庣矆娓氣偓楠炲鏁撻悩鍐蹭画濡炪倖鐗楃粙鎾汇€呴崣澶岀瘈濠电姴鍊绘晶鏇㈡煕鐏炶濮傞柡宀€鍠撻埀顒傛暩椤牊鐗庡┑鐘愁問閸ㄧ敻宕戦幇顔筋潟闁圭儤鏌￠崑鎾绘晲鎼粹€茬盎濡炪倖娲樼划鎾澄涢崨鎼晢濠㈣泛锕ら～顐ょ磽娴ｄ粙鍝洪悽顖涘浮閸┿儲寰勬繝搴㈠缓闂佺硶鍓濋…鍕Ω閿旇法绠氶梺闈涚墕濞层倕鐡繝鐢靛仜濡﹪宕㈡總鍛婂仼鐎瑰嫭瀚堥弮鈧幏鍛存偡闁腹鍋撻幘鍓佺＝濞达絽婀遍埥澶嬨亜閹存繃鍤囩€殿喗鐓￠幃鈺冪磼濡攱瀚兼繝娈垮枤閹虫挸煤閵堝棔绻嗗┑鍌氭啞閸婂灚鎱ㄥΟ鐓庡付鐎殿喓鍔嶇换娑㈠礂閸忚偐顦ㄧ紓浣虹帛缁嬫帒顭囪箛娑樼鐟滃秹宕哄畝鈧槐鎾存媴閸撳弶鈻堝銈冨劜閹瑰洭宕洪埀顒併亜閹哄棗浜鹃梺璇茬箲瀹€鎼佸箖瑜旈幃鈺呮嚑椤掍焦顔曢梻浣稿閸嬪懎煤閺嶎偆妫憸鏃堝蓟閻旇　鍋撳☉娆樼劷缂佺姵鐗犻弻锟犲幢閹邦剙濮﹀┑顔硷龚濞咃綁骞冩导鏉戠伋闁稿繐鎳庨ˉ姘舵⒒閸屾瑦绁版い鏇熺墵瀹曡鎯旈妸銉ь槰闂侀潧顭粻鎺旀閵忋倖鈷掗柛灞剧懆閸忓瞼绱掗鍛仸濠碉繝娼ч埥澶愬閻樺灚鐓ｆ繝鐢靛仦閸垶宕归崷顓犱笉?
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
    
    // ========== Mounted Turret Types ==========
    
    public TurretBaseBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.TURRET_BASE.get(), pos, state);
        if (getTier() >= 5) {
            builtInSmartChip = new ItemStack(ModItems.SMART_CHIP.get());
        }
    }
    
    // ========== Mounted Turret Types ==========
    
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

    private ItemStack getBuiltInSmartChipIfPresent() {
        if (!hasBuiltInSmartChip()) {
            return ItemStack.EMPTY;
        }
        if (builtInSmartChip.isEmpty() || !(builtInSmartChip.getItem() instanceof SmartChipItem)) {
            return ItemStack.EMPTY;
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
     * 闂傚倸鍊搁崐椋庣矆娓氣偓瀹曘儳鈧綆鍠栫壕鍧楁煙閹増顥夐幖鏉戯躬閺屻倝鎳濋幍顔肩墯婵炲瓨绮岀紞濠囧蓟濞戙垹唯妞ゆ梻鍘ч～鈺冪磽娴ｅ搫啸闁轰礁顭峰濠氭偄閻撳簼绱堕梺鍛婃处閸樻粓鏁愰崱鎰盎闂佸綊鍋婇崹鎷岊暱濠电儑绲藉ú銈夋晝椤忓嫮鏆﹂柨婵嗘缁剁偛鈹戦悩鎻掝仹闁哥姴锕︾槐鎾诲磼濮橆兘鍋撻崫銉х煋闁圭虎鍠栫粻鐘充繆椤栨瑨顒熼柛銈嗘礃閵囧嫰骞囬崜浣稿煂濡炪倖娲熸禍鍫曞蓟閳╁啫绶為悗锝庝簽娴犵顪?
     * T1: 闂傚倸鍊搁崐椋庣矆娓氣偓楠炴牠顢曢敃鈧悿顕€鏌ｅΔ鈧悧濠囧矗韫囨拋褰掓晲閸涱厽娈ф繛瀛樼矊缂嶅﹪寮诲☉銏犵疀闁靛闄勯悵鏍ь渻閵堝倹娅囬柛蹇旓耿瀵濡舵径濠傜獩婵犵數濮撮崯顖炴偟?
     * T2-T3: 1婵犵數濮烽弫鎼佸磻閻愬搫鍨傞柛顐ｆ礀缁犲綊鏌嶉崫鍕偓濠氥€呴崣澶岀瘈闂傚牊渚楅悞鎯瑰鍐Ш闁哄本鐩獮鍥Ω閿旂晫褰囨俊鐐€愰弲婵嬪礂濮椻偓瀵濡舵径濠傜獩婵犵數濮撮崯顖炴偟?
     * T4-T5: 2婵犵數濮烽弫鎼佸磻閻愬搫鍨傞柛顐ｆ礀缁犲綊鏌嶉崫鍕偓濠氥€呴崣澶岀瘈闂傚牊渚楅悞鎯瑰鍐Ш闁哄本鐩獮鍥Ω閿旂晫褰囨俊鐐€愰弲婵嬪礂濮椻偓瀵濡舵径濠傜獩婵犵數濮撮崯顖炴偟?
     */
    public int getPluginSlotCount() {
        return switch (getTier()) {
            case 1 -> 0;
            case 2, 3 -> 1;
            case 4, 5 -> 2;
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
     * 闂傚倸鍊搁崐椋庣矆娓氣偓瀹曘儳鈧綆鍠栫壕鍧楁煙閹増顥夐幖鏉戯躬閺屻倝鎳濋幍顔肩墯婵炲瓨绮岀紞濠囧蓟濞戙垹唯妞ゆ棁宕甸弳妤佺箾鐎涙鐭婄紓宥咃躬瀵鎮㈤悡搴ｇ暰閻熸粌绉瑰铏綇閵婏絼绨婚梺闈涚墕閹冲繘宕甸崶顒佺厵妞ゆ梻鐡斿▓鏃堟煃閽樺妲搁柍璇茬Ч椤㈡顦遍梺顓у灣閳ь剝顫夊ú姗€宕归悽鍓叉晣闁稿繒鍘х欢鐐烘倵閿濆簼绨诲鐟板濮婄粯鎷呴崨濠傛殘濠电偠顕滅粻鎾崇暦娴兼潙鍐€妞ゆ挾鍋涢崑宥夋⒑閸︻叀妾搁柛鐘愁殜閸?
     */
    public int getEnergyStored() {
        return energyStorage.getEnergyStored();
    }
    
    /**
     * 闂傚倸鍊搁崐椋庣矆娓氣偓瀹曘儳鈧綆鍠栫壕鍧楁煙閹増顥夐幖鏉戯躬閺屻倝鎳濋幍顔肩墯婵炲瓨绮岀紞濠囧蓟濞戙垹唯妞ゆ梻鍘ч～鈺呮煟鎼淬垼澹樻い锔炬暬瀵顓奸崼顐ｎ€囬梻浣告啞閹稿鎳濇ィ鍐炬晪闁靛鏅涚粈瀣亜閺囩偞鍣洪柍璇差樀濮婄粯鎷呮笟顖滃姼缂備胶绮崹鍧楀极閸愵喖顫呴柕鍫濇嚀閹芥洟鎮楅崗澶婁壕闂佸憡鍔︽禍鐐哄级閹间焦鈷戦悷娆忓缁€鍐煕閳哄倻澧垫鐐差槹缁轰粙宕ㄦ繛鐐?
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
        // 闂傚倷鑳堕…鍫㈡崲閹扮増鍋嬮柟浼村亰閺佸霉閻撳海鎽犻柛搴㈠灴閺岋綁寮崼顐ｎ棖闂佸搫顦划娆撳蓟閵堝棛鐟规い鏍ㄦ皑娴狀垱绻濋埛鈧崟顓炩拰闂佽桨绀佺粔鍫曞焵? 濠电姷鏁搁崑娑欏緞閸ヮ剙绀堟繝闈涙４閼板灝銆掑锝呬壕闂佽鍨板ú顓㈠箖閵忊槅妲归幖娣灪缁朵即姊绘笟鈧Λ璺ㄦ媰閿曞倸鍨傞柧蹇ｅ亝濞呯娀鏌″搴″箹缂侇偄绉归幃褰掑传閸曨剚鍎撻梺鍝勬噺閻熲晠寮诲☉銏犖ㄧ憸宥嗙闁秵鐓冪憸婊堝礂濮椻偓瀹曟垿骞樼紒妯煎幈闂佸湱鍎ら幐鑽ゆ閺屻儲鐓曢柣鎰▕閸ょ喓绱掓潏銊︻棃妞ゃ垺鐟╁畷褰掝敊瑜忛崰鏍蓟閿濆憘鏃堝焵椤掆偓鐓ら柕蹇嬪€曢悞?
        int slotCount = Math.min(getPluginSlotCount(), basePluginSlot.getSlots());
        for (int i = 0; i < slotCount; i++) {
            ItemStack stack = basePluginSlot.getStackInSlot(i);
            if (!stack.isEmpty() && stack.getItem() instanceof SmartChipItem) {
                return stack;
            }
        }
        return getBuiltInSmartChipIfPresent();
    }
    
    /**
     * 闂傚倸鍊风粈渚€宕ョ€ｎ喖纾块柟鎯版鎼村﹪鏌ら懝鎵牚濞存粌缍婇弻娑㈠Ψ椤旂厧顫╃紓浣哄О閸庣敻寮诲鍫闂佸憡鎸鹃崰搴敋閿濆鍨傛い鎰╁灮缁愮偤鏌ｆ惔顖滅У濞存粎鍋熺划濠囨煥鐎ｎ剛顔曢柣搴ｆ暩椤牓鐎锋俊鐐€栧褰掑礉濡も偓鍗遍柟閭﹀厴閺嬪酣鏌熼悙顒佺稇濞寸姵妞藉娲濞戣京鍔搁梺绋匡攻椤ㄥ﹪骞婂┑瀣畾鐟滄粓鎮㈤崱娑欑厾闁告縿鍎查弳鈺傘亜閿濆牆鐨洪柟鍙夋倐瀵爼宕归鑺ヮ唹缂傚倷绀侀崐鍦暜閻愬搫绠查柛鏇ㄥ灠娴肩娀鏌涢弴銊ュ箻闂夊姊婚崒娆戭槮闁规祴鈧剚娼栫紓浣股戦崣蹇涙倵濞戞鎴﹀矗韫囨梻绠鹃柟瀛樼懃閻忣亪鏌涢妶鍛伃闁哄矉绻濆畷鎺懳熺悰鈥充壕婵°倕鍟伴惌?
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
        ItemStack builtInChip = getBuiltInSmartChipIfPresent();
        if (!hasRealSmartChip() && !builtInChip.isEmpty()) {
            plugins.add(builtInChip);
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
        if (stack.isEmpty() && hasBuiltInSmartChip()) {
            stack = getOrCreateBuiltInSmartChip();
        }
        if (!stack.isEmpty()) {
            com.tian_nu.AdvancedTurret.items.SmartChipItem.setFriendlyFire(stack, friendlyFire);
            setChanged();
        }
    }

    public void setPredictiveAiming(boolean predictiveAiming) {
        ItemStack stack = getPluginStack();
        if (stack.isEmpty() && hasBuiltInSmartChip()) {
            stack = getOrCreateBuiltInSmartChip();
        }
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

    public String getOwnerName() {
        return ownerName;
    }

    public String getResolvedOwnerName() {
        if (!ownerName.isBlank()) {
            return ownerName;
        }
        return resolveOwnerNameFromLevel();
    }

    public String getCachedOwnerName() {
        if (!ownerName.isBlank()) {
            return ownerName;
        }
        return resolveOwnerNameFromLevel();
    }

    private String resolveOwnerNameFromLevel() {
        if (owner != null && level != null) {
            Player player = level.getPlayerByUUID(owner);
            if (player != null) {
                return player.getName().getString();
            }
        }
        return "";
    }

    public void setOwner(java.util.UUID owner, @Nullable String ownerName) {
        this.owner = owner;
        this.ownerName = ownerName == null ? "" : ownerName;
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
     * 闂傚倸鍊峰ù鍥х暦閸偅鍙忛柡澶嬪殮濞差亶鏁囬柕蹇曞Х閸濇姊绘笟鍥у缂佸鏁诲畷鏇㈠箣閿旂晫鍘靛┑鐐茬墕閻忔繈寮搁幘缁樼厱閻庯綆鍋呭畷宀勬煟濞戝崬娅嶇€规洖缍婇、娆撳矗閵壯勶紡闂傚倸鍊烽懗鍓佹兜閸洖鐤炬繝濠傛噽閻瑩鏌熼幑鎰【闁搞劍绻堥弻宥夊传閸曨剙顎涢梺杞扮缁夌數鎹㈠☉銏犵闁绘劕鐏氶崳顕€姊洪崨濠傜仯缂侇喗鐟ラ～?
     */
    public void requestClientUpdate() {
        syncToClient();
    }
    
    // ========== Mounted Turret Types ==========
    
    public static void tick(Level level, BlockPos pos, BlockState state, TurretBaseBlockEntity blockEntity) {
        if (level.isClientSide) return;

        blockEntity.ensureEnergyCapacity();
        
        boolean changed = false;
        
        // 濠电姷鏁告慨鐑姐€傞挊澹╋綁宕ㄩ弶鎴狅紱闂侀€炲苯澧撮柡灞剧〒閳ь剨缍嗛崑鍛暦瀹€鍕厸鐎光偓鐎ｎ剛锛熸繛瀵稿婵″洭骞忛悩璇茬闁圭儤鍩堝娑㈡⒒閸屾瑧鍔嶉柟顔肩埣瀹曟繈骞嬮悙娈挎锤闂佸壊鍋呭ú鏍嫅閻斿吋鐓曟繛鎴烆焽閹界娀鏌涚€ｎ偄濮嶉柡灞剧☉閳规垿宕卞Δ濠佹偅婵°倗濮烽崑娑㈩敄婢跺娼栫紓浣股戞刊鎾煟閻旂顥嬬紒渚€顥撶槐鎾存媴閾忕懓绗￠柣銏╁灣閸嬨倕鐣峰璺虹闁规惌鍘奸幃鎴︽⒑缁洖澧查柣鐕佸灠闇?
        if (blockEntity.hasCreativePowerComponent()) {
            int maxEnergy = blockEntity.energyStorage.getMaxEnergyStored();
            if (blockEntity.energyStorage.getEnergyStored() < maxEnergy) {
                // 闂傚倸鍊搁崐鐑芥嚄閸洖纾块柣銏㈩焾閻ょ偓绻涢幋娆忕仾闁稿鍊濋弻鏇熺箾瑜嶇€氼厼鈻撴导瀛樼厽閹兼番鍔嶅☉褔鏌熷ù瀣⒉闁告帒锕ョ缓浠嬪川婵犲嫬骞堥梻浣告贡閸嬫捇銆冮崨鏉戠疇闊洦绋掗崐鍨亜閹捐泛浜归柡鈧禒瀣厽婵☆垳鍎ら埢鏇㈡煟閹垮啫骞樼紒杈ㄥ笚濞煎繘濡搁敂缁㈡Ч闁诲孩顔栭崳顕€宕戞繝鍥╁祦闁哄秲鍔嶆刊鎾偡濞嗗繐顏柣婵愬亰濮婄粯鎷呯粙娆炬闂佺顑勭欢姘暦鐟欏嫮绡€婵﹩鍓涢鎰版椤愩垺澶勭紒瀣灩閻ヮ亣顦归柡灞界У濞碱亪骞嶉鐓庮瀴婵＄偑鍊曠€涒晠鎮伴弨鍍veEnergy闂傚倸鍊搁崐鐑芥倿閿曞倹鍎戠憸鐗堝笒缁€澶屸偓鍏夊亾闁告洦鍋呭Σ顒勬⒑绾懎鍚规い銏℃懌eceive闂傚倸鍊搁崐鎼佸磹閹间礁纾归柛婵勫劗閸嬫挸顫濋悡搴＄睄閻庤娲樼换鍫濐嚕閹绢喖顫呴柣妯挎珪琚ｅ┑鐘茬棄閺夊簱鍋撻弴銏犵疇闊洦绋戦悿?
                blockEntity.setEnergyFull();
                changed = true;
            }
        } else {
            // 婵犵數濮烽弫鍛婃叏娴兼潙鍨傚ù鐘茬憭閻戣棄纾兼繛鎴炲嚬濞茬顪冮妶鍛閻庢埃鍋撻梺鎸庣箓濡盯姊介崟顖涚厱婵炴垶锕銉︾箾閸喎鐏存慨濠冩そ瀹曟姊荤壕瀣劵闂備胶顭堝锔界椤掆偓椤曘儲绻濋崟顒€鐝伴柣鐔舵缁€浣虹礊婵犲洤绠栭柍鍝勬噺閸婄粯淇婇婊庡劆闁逞屽墯閸旀瑩骞冨畡閭︾叆闁告洦鍓涢崙锟犳⒑缁嬪尅宸ユ繛纭风節閻?
            // 闂傚倸鍊搁崐椋庣矆娓氣偓楠炴牠顢曚綅閸ヮ剦鏁冮柨鏇楀亾闁绘帒鐏氶妵鍕箳閸℃ぞ澹曢柣搴㈩問閸犳牠鎮ユ總鎼炩偓渚€寮撮悢渚祫闁诲函缍嗛崑鍡涘矗閸℃せ鏀介柣鎰綑閻忥箓鏌ｉ悤鍌滅暤闁诡垰鍟撮獮瀣倷閻㈡鍟庨梻浣告啞缁嬫垵煤閵堝洠鍋撳鐓庡婵?婵?闂傚倸鍊搁崐鐑芥嚄閸撲礁鍨濇い鏍仜缁€澶愭煥閺囩偛鈧摜绮ｅΔ鍛厸鐎广儱楠搁獮鎴︽煃瑜滈崗娑氱矆娓氣偓閸┿垺鎯旈妸銉х杸濡炪倖鎸鹃崕鎰板焵椤掍焦宕岄柟顔筋殜閻涱噣宕归鐓庮潛婵犵數鍋涢惇浼村礉閹存繍鍤曢柟闂寸绾惧吋鎱ㄥ鍫㈢ɑ妞ゎ偄绉瑰娲偡閺夊簱鎸冨銈嗗笚閻撯€愁嚕閵婏妇绡€婵﹩鍘鹃崢鎼佹倵閸忓浜鹃梺閫炲苯澧寸€规洑鍗冲鍊燁槾闁哄棴绠撻弻銊モ攽閸♀晜鈻撻梺杞扮缁夌數鎹㈠☉銏犲耿婵°倓鐒﹀畷鎶芥⒒閸屾凹妲哥紒澶屾嚀椤繘宕崝鍊熸閹峰鐣烽崶鈺傛櫒濠碉紕鍋戦崐鏍垂閻㈢绠犳俊顖濇閺嗭箓鏌ц箛娑掑亾濞戞瑥濯伴梻浣筋嚃閸ㄥ酣宕ㄩ婊冪ウ闂傚倸鍊风粈渚€宕崸妤€绠规い鎰剁畱閻ゎ噣鏌ｅΟ娆惧殭闁?
            if (blockEntity.hasSolarPlugin()) {
                boolean isDaytime = level.getDayTime() % 24000 < 12000; // 0-12000闂傚倸鍊搁崐椋庣矆娓氣偓楠炴牠顢曢妶鍌氫壕婵ê宕崢瀵糕偓瑙勬礀缂嶅﹤鐣峰Ο濂藉湱鈧綆鍋嗛埀顒佹そ閹嘲顭ㄩ崘顕呪偓妤併亜?
                // 濠电姷鏁告慨鐑姐€傞挊澹╋綁宕ㄩ弶鎴狅紱闂侀€炲苯澧撮柡灞剧〒閳ь剨缍嗛崑鍛暦瀹€鍕厸鐎光偓鐎ｎ剛锛熸繛瀵稿婵″洭骞忛悩璇茬闁圭儤鍩堝娑㈡⒒閸屾瑨鍏岀紒顕呭灦瀵鏁撻悩鑼紱闂佺懓澧界划顖炴偂濞嗘挻鐓熼柟鎯у暱椤掋垽鏌熼悷鎵煟闁哄本绋撴禒锕傚礈瑜忛悡鍌炴⒑閻熺増鈻曞ù婊庝簻椤繐煤椤忓嫪绱堕梺鍛婃处閸嬧偓闁稿鎹囧畷绋课旈埀顒勬倿閸偁浜滈柟鍝勭Х閸忓矂鏌曢崱妯虹瑲闁靛洤瀚粻娑樷槈濡や礁鏋ら梻浣告惈婢跺洭鍩€椤掍礁澧柛姘儔閺屾稑鈽夐崡鐐茬闂佺顭崹浼村煘閹达箑鐏抽柛鎰皺妤犲洭姊虹拠鈥虫灈闁稿﹥鎮傞幃楣冩倻閽樺）銊╂煃閸濆嫬鈧懓鈻嶉崶銊х閺夊牆澧介幃鍏笺亜椤撶偟澧﹂柣娑卞枤閳ь剨缍嗛崰妤呭煕閹寸偟绠鹃柤濂割杺閸炶櫣绱掗妸褏甯涢柕鍥у楠炲鈹戦崶褎鐣婚柣搴ゎ潐濞叉繂鈻嶉敐鍡欘浄闁挎洖鍊哥粻姘辨喐韫囨洜鐜婚柣鎰劋閳锋垿鏌涢幇顒€绾ч柟顖氱墦閺屾盯鎮㈤崫鍕ㄦ瀰閻庢鍣崑濠傜暦閻旂⒈鏁嶆繛鎴炶壘瀵櫕绻濋悽闈涗沪闁搞劌鐖奸垾锕傚炊閵婏附鐝烽梺鍝勬川閸犳挾绮绘ィ鍐╃厱婵炲棗娴氬Σ娲煟椤撶儐鍎戠紒杈ㄥ笧閳ь剨缍嗘禍璺何熼埀顒勬倵鐟欏嫭澶勯柛瀣工閻ｇ兘鎮㈢喊杈ㄦ櫍闂佺粯鍔曢悺銊モ枔閵忋倖鈷掑ù锝呮啞鐠愶繝鏌涘Ο鐘插幘濞差亝鍊婚柦妯侯槺閿涙粌鈹戦悩璇у伐闁瑰啿绻樺畷顖涘鐎涙ǚ鎷绘繛杈剧到閹诧繝骞嗛崼銉︾厱濠电姴鍋嗛悡鍏碱殽閻愯韬鐐达耿閻涱喗寰勯崨濠勬綎闂傚倷绀侀幖顐ゆ偖椤愶箑纾块柡灞诲劚閸氬綊鏌涢弴銊ョ仭闁绘挾濞€閺岀喖顢橀悢椋庣懆闂佸憡姊圭划搴ｆ閹烘鍋愰柛鎰级鐠囩偤姊洪崫鍕効缂傚秳绶氶悰顕€宕堕渚囨濠电偞鍨堕悷褎绂嶉鍛箚闁绘劦浜滈埀顒佸灴瀹曠懓煤椤忓懎浠梺鍐叉惈閹冲酣寮?
                boolean canSeeSky = level.canSeeSky(pos.above());
                if (isDaytime && canSeeSky) {
                    int generated = com.tian_nu.AdvancedTurret.Config.solarEnergyGeneration;
                    int added = blockEntity.addEnergyDirectly(generated);
                    if (added > 0) {
                        changed = true;
                    }
                }
            }
            
            // 缂傚倸鍊搁崐鎼佸磹瀹勯偊娓婚柟鐑橆殔缁€瀣叏濡炶浜鹃悗瑙勬礉椤鎹㈠┑鍡╂僵妞ゆ挾鍋樼花鐢告⒒娴ｈ棄浜规い顓炵墦閹ê顫濋鐔峰伎濠电姴锕ょ€氥劍绂嶅鍫熺厵闁告繂瀚ˉ婊兠瑰鍫㈢暫闁哄矉绲鹃幆鏃堟晬閸曨亞椹抽梻浣虹帛閹碱偆鎹㈠┑瀣仒妞ゆ洍鍋撴鐐搭焽閹风娀鎳犻澶婃暯闂佽楠哥粻宥夊磿閸楃倣娑㈩敇閵忕姷锛涢梺闈浥堥弲婊堝煕閹达附鐓欑紒瀣閹癸綁鏌嶉娑欑闁哄矉缍侀、妯款槻闁哄鍊濋弻娑㈠箳閹捐櫕璇為梺璇″枓閺呮盯顢欒箛娑辨晩闁稿繒鈷堥崥娆戠磽閸屾瑨鍏岄柧蹇撻叄瀹曘垺绺界粙璺ㄧ枃闁硅壈鎻徊鍧楁儗閸儲鐓冮柛婵嗗閳ь剟顥撻幑銏ゅ幢濞戞瑧鍘鹃梺鐓庢贡婢ф娑甸崜褎鍋栨慨妯夸含绾捐棄霉閿濆懎顥忔俊鍙夋そ閺屾稖绠涢弮鎾光偓鎸庮殽閻愯韬鐐疵…鍧楀礋椤掍緡妫冮悗瑙勬礃閿曘垽鍨鹃敃鍌氱婵犻潧鏌堥妸鈺傗拻?缂傚倸鍊搁崐鎼佸磹瀹勯偊娓婚柟鐑橆殔缁€瀣叏濡炶浜鹃悗瑙勬礉椤鎹㈠┑鍡╂僵妞ゆ挾鍋樼花鐢告⒒娴ｇ顥忛柛瀣噹鐓ら柡宥庡幗閻撱儵鏌曢崼婵囶棛缂佽妫濋弻鏇㈠醇濠靛洤娅ｅ銈呮禋娴滅偟妲愰幒鎾剁懝闁搞儜鍕綆闂備礁鎼惉濂稿窗閹捐绠柣妯款嚙閻掓椽鏌涢幇鍏哥敖闁绘稏鍨藉娲嚒閵堝憛锝夋煕閺冣偓椤ㄥ﹤鐣锋导鏉戝唨妞ゆ挾鍋熼悿鍥р攽鎺抽崐鎾绘嚄閸洏鈧懘鎮滈懞銉モ偓鐢告煥濠靛棛鍑圭紒銊ュ悑閵?
            if (blockEntity.hasRedstoneConversionPlugin()) {
                int energyPerRedstone = com.tian_nu.AdvancedTurret.Config.redstoneToEnergyRatio;
                int energyPerRedstoneBlock = 18000; // 缂傚倸鍊搁崐鎼佸磹瀹勯偊娓婚柟鐑橆殔缁€瀣叏濡炶浜鹃悗瑙勬礉椤鎹㈠┑鍡╂僵妞ゆ挾鍋樼花鐢告⒒娴ｇ顥忛柛瀣噹鐓ら柡宥庡幗閻撱儵鏌曢崼婵囶棛缂佽妫濋弻鏇㈠醇濠靛洤娅ｅ銈呮禋娴滅偟妲愰幒鎾剁懝闁搞儜鍕綆闂備礁鎼張顒勬儎椤栨凹鍤曞ù鐘差儏鎯熼悷婊冪Ч閸╂稒绻濋崶銊㈡嫼缂備礁顑堝▔鏇犵不閺屻儲鐓ラ柡鍥悘鑼偓?
                int maxEnergy = blockEntity.energyStorage.getMaxEnergyStored();
                int currentEnergy = blockEntity.energyStorage.getEnergyStored();
                int space = maxEnergy - currentEnergy;
                
                // 婵犵數濮烽弫鎼佸磻閻愬樊鐒芥繛鍡樻尭鐟欙箓鎮楅敐搴℃灍闁哄拋浜缁樻媴閸涘﹤鏆堥柦鍐含缁辨帞鈧綆鍋勯悘鎾煕閳瑰灝鍔滅€垫澘瀚伴獮鍥敆閸曨偅鏆梻鍌欑劍鐎笛呮崲閸岀偛纾归柛娑橈功椤╂煡鏌ｉ幇闈涘幐缂佽妫濋弻鏇㈠醇濠靛洤娅ｉ梺鍝勬閻燂箓濡甸崟顖氱閻庯綆浜炴导鍫ユ⒑閸濆嫮鐒跨紓宥勭窔閵嗕礁顫滈埀顒勫箖濞嗘挸鐭楀璺侯儑濡差亜鈹戦悩鍨毄闁稿鍠栨俊瀵糕偓锝庡墯瀹曞弶绻濋棃娑卞剰闁哄鑳堕埀顒€绠嶉崕閬嵥囬鐐寸厑闁搞儺鍓氶悡銉︾節闂堟稒顥為柛锝堫潐椤ㄣ儵鎮欏顔煎壎濠殿喖锕︾划顖炲箯閸涘瓨鍋￠柟娈垮枤閵堥绱撻崒娆掑厡缂侇噮鍨跺畷褰掓偨闂堟稑鐤鹃梻鍌欑濠€閬嶁€﹂崼鐕佹闁告縿鍎查浠嬫煟閹邦剛鎽犻柛娆忕箲娣囧﹪顢涘顓熷創闂佹娊鏀卞鑽ゆ閹烘鐒?
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
                
                // 闂傚倸鍊搁崐椋庣矆娓氣偓楠炲鏁撻悩鑼槷闂佸搫娲ㄦ慨鐑芥儗閹剧粯鐓熼柕蹇嬪灪閺嗏晠鏌ｉ鐔稿磳闁哄矉缍侀獮瀣晲閸涘懏鎹囬弻娑㈠Χ閸屾矮澹曟繝鐢靛Т閻ュ寮舵惔鎾充壕闁哄洢鍨归悿顔姐亜閹拌泛鐦滈柡浣告閺屾洝绠涚€ｎ亖鍋撻弽顓熷亗闁绘梹鎮舵禍婊堟煙閹规劖纭惧ù鐘欏洦鐓ユ繛鎴烆焽鏁堥梺?
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
        
// 闂傚倸鍊搁崐鐑芥嚄閸撲礁鍨濇い鏍仜缁€澶愭煥閺囩偛鈧摜绮ｅΔ鍛厸鐎广儱楠搁獮鎴︽煃瑜滈崗娑氱矆娓氣偓閸┿垺鎯旈妸銉ь啋闁诲海鏁搁…鍫熸叏閻戣姤鈷掑ù锝呮啞閹牓鎮楀鐓庡闁瑰箍鍨介獮鎺楀籍閳ь剟寮冲鍫熺厱闁斥晛鍟伴埊鏇㈡煟閹捐泛鏋戠紒缁樼箖缁绘繈宕掑闂寸棯缂傚倷娴囧▔娑橆嚕閸撲焦宕叉繝闈涱儏閻掑灚銇勯幒鎴濐伌闁轰礁鍊块弻娑㈩敃閵堝懏鐎绘繛瀵稿У濡炰粙寮婚埄鍐ㄧ窞濠电姴瀚。鐢告⒑閸濆嫭鍣洪柣鈺婂灦閵嗕線寮崼顐ｆ櫆闂佺硶鍓濋悷褔鏁嶅鍫熲拺闁告繂瀚埀顒€缍婇、鏍р枎閹惧磭锛熼梺鐟邦嚟閸嬬喓绮绘ィ鍐╃厱妞ゆ劑鍊曢弸搴ㄦ煟韫囨梹灏﹂柡宀€鍠栭、娆撴偩鐏炴儳娅氭俊鐐€戦崹娲偡瑜斿畷鐗堢節閸愨晝绉堕梺闈涱煭闂勫嫰鎮甸敃鍌涒拻闁稿本鐟чˇ锕傛煙绾板崬浜扮€规洘鍔栫换婵嗩潩椤掑倻鏋冮梻浣告贡閸嬫捇宕滃鑸靛€垮ù鐘差儐閻撱儲绻涢幋鐏活亪顢旈妶澶嬬厱閻庯綆鍋呭畷宀€鈧娲滈崗姗€銆佸鈧幃娆撳箥椤曗偓閹歌偐绱撻崒姘偓鎼佸磹閸濄儳鐭撻柟缁㈠櫘閺佸嫰鏌涢埄鍐炬闁搞倖顨婇弻娑㈠即閵娿儳浠╃紓浣插亾閻庯綆鍋佹禍婊堟煙閹规劖纭鹃柡瀣叄閺?
		// 闂傚倸鍊搁崐鐑芥嚄閸撲焦鍏滈柛顐ｆ礀閻ょ偓绻涢幋娆忕仼缂佺姾顫夐妵鍕箛閸撲胶校濠电偛鐗呯划娆撳蓟濞戙垹唯妞ゆ梻鍘ч～鈺呮⒑濞茶骞楁い銊ユ婵＄敻宕熼鍓ф澑闂佸湱鍋撳娆戠玻濞戞﹩娓婚柕鍫濈箳缁变即鏌涘Δ浣糕枙妤犵偛鍟抽妵鎰板箳閹寸姷鏉搁梻浣告啞閹哥兘鎳楅幆鐗堟噷闂備浇宕甸崰鎰垝閹炬眹鈧倿鏁冮崒娑樷偓鍧楁煥閺傚灝鈷旂紒鐘虫閺岋綁寮捄銊︻唸濠碘剝褰冮悧濠勬崲濠靛洨绡€闁稿本纰嶅▓顓炍旈悩闈涗粶妞ゆ垵顦～?

		// 濠电姷鏁告慨鐑藉极閹间礁纾婚柣鎰惈缁犱即鏌熼梻瀵割槮缂佺姷濞€閺岀喖鎮ч崼鐔哄嚒缂備胶濮甸悧鏇㈠煘閹达附鍋愰柟缁樺笚濞堝弶绻涚€涙ê娈犻柛濠冪箞瀵鈽夐姀鐘栥劑鏌ㄥ┑鍡樺櫣妞ゎ剙顦靛铏瑰寲閺囩喐鐝旈梺绋匡工濞尖€愁嚕婵犳碍鏅插璺好￠埡鍛厪濠㈣泛鐗嗛崝鏉懨归悩鍨殌闁宠鍨块幃鈺佲枔閹稿孩鐦滈梻浣侯焾椤戝棝骞戦崶顒€钃熼柨婵嗘閸庣喖鏌曢崼婵嗩劉缂傚秴鐗婄换婵嬫偨闂堟稐绮堕梺鎸庢处娴滎亝淇婇幘顔肩＜闁绘劘灏欓崢鎼佹⒑閸涘﹤濮﹀ù婊勭箞閺?
		blockEntity.clearExpiredReservations(level.getGameTime());

		if (changed) {
            blockEntity.setChanged();
            blockEntity.syncToClient();
        }
    }
    
    // ========== Mounted Turret Types ==========
    
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
     * 濠电姷鏁告慨鐑姐€傞挊澹╋綁宕ㄩ弶鎴狅紱闂侀€炲苯澧撮柡灞剧〒閳ь剨缍嗛崑鍛暦瀹€鍕厸鐎光偓鐎ｎ剛锛熸繛瀵稿婵″洭骞忛悩璇茬闁圭儤鍩堝銉モ攽閻樻鏆柍褜鍓欓崯璺ㄧ棯瑜旈弻鐔碱敊閻撳簶鍋撻幖浣瑰仼闁绘垼妫勫敮闂佸啿鎼崐鐟扳枍閸℃稒鈷戦柛蹇涙？閼割亪鏌涙惔銏犫枙闁诡噣绠栭幖褰掝敃閵堝洨妲囬梻渚€娼х换鍫ュ磹閵堝绾ч柛婵嗗▕閻熼偊鐓ラ幖瀛樼箘閻╁酣鏌涘Δ鈧畷顒勫煘閹达箑纾兼慨姗嗗幖閺嗗牓姊洪幖鐐测偓鏇犫偓姘嵆瀵濡舵径濠勭暢闂佸湱鍎ら崹鍨叏鎼达絿纾藉ù锝堟閽勫吋銇勯弴鍡楁处缁?
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
     * 濠电姷鏁告慨鐑姐€傞挊澹╋綁宕ㄩ弶鎴狅紱闂侀€炲苯澧撮柡灞剧〒閳ь剨缍嗛崑鍛暦瀹€鍕厸鐎光偓鐎ｎ剛锛熸繛瀵稿婵″洭骞忛悩璇茬闁圭儤鍩堝銉モ攽閻樻鏆柍褜鍓欓崯璺ㄧ棯瑜旈弻鐔碱敊閻撳簶鍋撻幖浣瑰仼闁绘垼妫勫敮闂佸啿鎼崐鐟扳枍閸℃稒鈷戦柛蹇涙？閼割亪鏌涙惔銏犫枙闁诡噣绠栭獮搴ㄦ嚍閵夈儮鍋撻崹顐ょ闁割偅绻勬禒銏ゆ煛鐎ｃ劌鈧牠濡甸崟顖氼潊闁绘瑥鎳庢导鎰版⒑娴兼瑧鎮奸柡浣筋嚙閻ｇ兘骞掑Δ浣糕偓鐑芥煠绾板崬澧柍褜鍓涢崑銈咁潖閾忓湱纾兼俊顖氭惈椤矂姊虹€癸附婢樻慨鍌溾偓娈垮枟婵炲﹪銆侀弮鍫濋唶闁绘柨鎼獮宥囩磽閸屾艾鈧兘鎮為敃鍌涙櫔缂傚倷鐒﹂〃鍛村磹閸︻厽宕?
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
     * 濠电姷鏁告慨鐑姐€傞挊澹╋綁宕ㄩ弶鎴狅紱闂侀€炲苯澧撮柡灞剧〒閳ь剨缍嗛崑鍛暦瀹€鍕厸鐎光偓鐎ｎ剛锛熸繛瀵稿婵″洭骞忛悩璇茬闁圭儤鍩堝銉モ攽閻樻鏆柍褜鍓欓崯璺ㄧ棯瑜旈弻鐔碱敊閻撳簶鍋撻幖浣瑰仼闁绘垼妫勫敮闂佸啿鎼崐鐟扳枍閸℃稒鈷戦柛蹇涙？閼割亪鏌涙惔銏犫枙闁诡喗鍎抽悾鐑藉炊閳哄喛绱冲┑鐐舵彧缁蹭粙宕查懠顑藉亾濮橆剟鍙勯柡灞剧洴婵℃悂濡烽鎯ф倯闂備浇顕栭崳顖滄崲濠靛绠犳繝濠傜墕閸ㄥ倹銇勯幇鈺佺労婵炵⒈鍨辨穱濠囨倷椤忓嫧鍋撻弽褜鍟呭┑鐘宠壘绾惧鏌熼崜褏甯涢柛濠勬暬閺屾盯鈥﹂幋婵呯敖闂佸憡鐟㈤崑鎾翠繆閵堝洤啸闁稿鍋ら獮鎴﹀炊椤掑倸绁﹂梺鍦劋閸╁牓宕ョ€ｎ喗鐓曢柍閿亾闁哄懏绋掔粋?     */
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
     * 濠电姷鏁告慨鐑姐€傞挊澹╋綁宕ㄩ弶鎴狅紱闂侀€炲苯澧撮柡灞剧〒閳ь剨缍嗛崑鍛暦瀹€鍕厸鐎光偓鐎ｎ剛锛熸繛瀵稿婵″洭骞忛悩璇茬闁圭儤鍩堝銉モ攽閻樻鏆柍褜鍓欓崯璺ㄧ棯瑜旈弻鐔碱敊閻撳簶鍋撻幖浣瑰仼闁绘垼妫勫敮闂佸啿鎼崐鐟扳枍閸℃稒鈷戦柛蹇涙？閼割亪鏌涙惔銏犫枙闁诡噣绠栭獮搴ㄦ嚍閵夈垺瀚兼繝鐢靛仩鐏忣亪顢氳瀵悂鎮㈤崫銉ь啎闂佺绻楅崑鎰板箠閸愨斂浜滄い鎾跺仦閸犳鈧鍠曠划娆撱€佸鈧幃娆忊枔閸喗鏉搁梻鍌氬€烽懗鍫曞箠閹捐绠规い鎰╁€楅惌鎾绘煟閵忕姴顥忛柡?
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
	 * 濠电姷鏁告慨鐑姐€傞挊澹╋綁宕ㄩ弶鎴狅紱闂侀€炲苯澧撮柡灞剧〒閳ь剨缍嗛崑鍛暦瀹€鍕厸鐎光偓鐎ｎ剛锛熸繛瀵稿婵″洭骞忛悩璇茬闁圭儤鍩堝銉モ攽閻樻鏆柍褜鍓欓崯璺ㄧ棯瑜旈弻鐔碱敊閻撳簶鍋撻幖浣瑰仼闁绘垼妫勫敮闂佸啿鎼崐鐟扳枍閸ヮ剚鈷戦梺顐ゅ仜閼活垱鏅剁€涙﹩娈介柣鎰级椤ョ偤鏌熼娑欘棃濠碘剝鎮傞弫鍐焵椤掍胶顩锋繛宸簼閳锋垿姊婚崼鐔剁繁闁绘帡绠栭弻娑欑節閸愮偓鐤侀悗瑙勬处閸ㄨ京鎹㈠┑瀣倞闁靛ě鍐ㄧ疄濠电姷顣藉Σ鍛村垂椤栨粍濯伴柨鏇楀亾閸楅亶鏌涜箛鏇ㄥ劆濞存粍绮撻弻鏇＄疀婵炴儳浜剧€规洖娲ｇ槐婵嬫⒒娴ｇ儤鍤€缁剧虎鍙冨畷鎴﹀Χ閸涱垱娈鹃梺鍦劋閸╁牓鎮￠妷鈺傜厱闁归偊鍘奸崝銈夋煛娴ｈ棄袚缂佺粯绻勯崰濠偽熷ú缁樼秹闂備礁澹婇悡鍫熺椤忓牆鏄ラ柍褜鍓氶妵鍕箳閸℃ぞ澹曠紓浣稿⒔閾忓酣宕㈤幆褉妲堥柣銏㈩焾閻?
	 */
	public boolean isThriftyMode() {
		ItemStack stack = getPluginStack();
		if (!stack.isEmpty()) {
			return com.tian_nu.AdvancedTurret.items.SmartChipItem.isThriftyMode(stack);
		}
		return false;
	}

	// ========== Mounted Turret Types ==========

/**
	 * 婵犵數濮烽。钘壩ｉ崨鏉戠；闁告侗鍙庨悢鍡樹繆椤栨氨姣為柛瀣尭椤繈鎮℃惔銏壕闂備線鈧偛鑻晶鍙夈亜椤愩埄妲圭紒缁樼⊕缁绘繈宕惰閻ｉ箖姊洪崨濠冨闁搞劌銈稿顐ｇ節閸ャ劎鍘介梺鎸庣箓閹虫挾鈧矮鍗抽弻锝堢疀濮樿泛鎽电紓浣虹帛缁嬫捇鍩€椤掑嫭娑у鐟帮躬瀹曟洟鎮╃紒妯煎幈婵犵數濮撮崯顖滅矆鐎ｎ偁浜?
	 * @param entityId 闂傚倸鍊搁崐鐑芥嚄閸洖纾块柣銏㈩焾閻ら箖鏌嶉崫鍕櫣缂佹劖顨嗘穱濠囧Χ閸涱喖娅ら梺绋款儏椤︾敻寮婚妸銉㈡斀闁糕剝顭囬ˇ閬嶆⒑缁嬫鍎愰柟鐟版搐閻ｇ兘鎮滅粵瀣櫍闂佺粯鍔栨竟鍡涙煢閻㈢數纾介柛灞捐壘閳ь剙鎽滅划鏃堝箻椤旇偐鍘存繝?	 * @param damage 婵犵數濮烽。钘壩ｉ崨鏉戠；闁告侗鍙庨悢鍡樹繆椤栨氨姣為柛瀣尭椤繈鎮℃惔锝勬婵＄偑鍊戦崹娲晝閵忕姷鏆﹂柕濠忓缁♀偓闂佺琚崐鏇炩枔婵傚憡鈷掑〒姘ｅ亾婵炰匠鍥ㄥ亱闁糕剝绋戠粻顖炴倵閿濆骸鏋涢柤?
	 * @param currentHealth 闂傚倸鍊搁崐鐑芥嚄閸洖纾块柣銏㈩焾閻ら箖鏌嶉崫鍕櫣缂佹劖顨嗘穱濠囧Χ閸涱喖娅ら梺绋款儏椤︾敻寮婚妸銉㈡斀闁糕剝鐟ラ。鑸电箾鐎涙鐭婄紓宥咃躬瀵鎮㈤悡搴ｇ暰閻熸粌绉瑰铏綇閵婏絼绨婚梺闈涚墕閹冲繘宕甸崶顒佺厸鐎光偓鐎ｎ剛袦婵犳鍠掗崑鎾绘⒑闂堟稓绠冲┑顔炬暬閹绻濆顓涙嫼闂侀潻瀵岄崣搴ㄥ礉濠婂懐纾肩紓浣诡焽缁犳捇鏌?
	 * @param gameTime 闂傚倷娴囧畷鐢稿窗閹邦喖鍨濋幖娣灪濞呯姵淇婇妶鍛櫣缂佺姳鍗抽弻娑樷槈濮楀牊鏁惧┑鐐叉噽婵炩偓闁哄矉绲借灒闁惧繗顫夐崟楣冩⒑闁偛鑻晶顖炴偨椤栨せ鍋撳畷鍥ㄦ闂侀潧顭俊鍥╁姬閳ь剟姊虹粙鎸庢拱缂侇喖鐗忛懞杈ㄧ鐎ｎ偀鎷绘繛杈剧到閹诧繝骞嗛崼銉︾厱闁绘洑绀佹禍浼存煙椤斿搫鍔﹂柟顕嗙節閻?
	 * @return 婵犵數濮烽。钘壩ｉ崨鏉戠；闁告侗鍙庨悢鍡樹繆椤栨氨姣為柛瀣尭椤繈鎮℃惔銏壕闂備線鈧偛鑻晶鍙夈亜椤愩埄妲圭紒缁樼⊕缁绘繈宕掑鍕啎闂備線娼ч¨鈧梻鍕瀹曟洟鎮㈤崗鑲╁弳濠电娀娼уΛ娆撍夐悩缁樼厽婵炴垶鐗曢崝锔芥叏婵犲啯銇濈€规洦鍋婂畷鐔碱敇婢跺牆鐏紒缁樼〒娴狅箓宕掑顒夌€抽梻浣哥－缁垶骞戦崶顒傚祦閻庯綆鍠楅弲婵嬫煃瑜滈崜娑氬垝閸儱閱囬柕澶涘閸樻捇鏌ｉ悙瀵糕枔闁逞屽墮閸樻牗绔熼弴鐘电＝闁稿本鑹鹃埀顒€鎽滅划鏃堟偨缁嬪灝鐎梺鍓茬厛閸ｎ噣宕甸弴銏＄厱妞ゎ厽鍨垫禍婊呯磼閸撲礁浠遍柟顔筋殜閺佹劖鎯旈垾鑼嚬闂備胶绮换鎰崲濠靛棭娼栭柛婵嗗珔瑜斿畷鎯邦槾濞寸厧瀚板娲传閵夈儛銏ゆ⒑鐢喚绉柣娑卞枟缁绘繈宕堕妸锔筋仧闂備胶绮…鍫焊濞嗘挻鏅繛鍡樺灩绾捐偐绱撴担闈涚仼鐎殿噮鍠楅妵鍕Ψ閿旇棄鏆楃紓渚囧枛閻楁挸鐣烽悢纰辨晬婵炴垶眉閸栨牠姊绘担鍝ョШ闁稿锕畷浼村冀椤撶偛鍤戦梺纭呮彧闂勫嫰鎮″☉銏＄厱闁斥晛鍟伴幊鍐煛鐎ｎ偅顥堥柡宀€鍠栭、娆戞嫚閹绘帞銈┑鐘殿暯閸撴繈鎮洪弴鈶哄洭寮跺Λ闈涚秺閹晛鐣烽崶銊ュ灡闂備礁鎼惌澶岀礊娓氣偓瀹曟椽鍩勯崘顏嗙槇闂佺鏈粙鎾寸閸洘鈷掑ù锝堟鐢盯鏌ｅΔ濠傚箻鐎垫澘锕ョ粋鎺斺偓锝呯仛閺呪晠妫呴銏″缂佸甯″畷鐢稿箣濠㈩亝鐩幃褔宕奸姀銏㈡毇闂備浇濮ら崹鐢杆囬棃娑辨綎婵炲樊浜滃婵嗏攽閻樻彃鈧粯绂掗懖鈺冪＝濞撴艾娲ゅ▍姗€鏌涢妸銈呭祮闁炽儻绠撳畷鍫曨敆閳ь剛绮堥崘鈹夸簻闁哄啫娲よ濠?
	 */
	public float reserveDamage(int entityId, float damage, float currentHealth, long gameTime) {
		// 缂傚倸鍊搁崐鎼佸磹妞嬪孩顐芥慨妯块哺閸忔粓鏌熼梻瀵割槮闁搞劌鍊块幃妤呭捶椤撶倫娑㈡煕濞嗘劕鍔ら柍瑙勫灴閸┿儵宕卞鍓у嚬婵犵數濮伴崹娲€﹂崶顒€鐒垫い鎺嶇閸ゎ剟鏌涢悩鍐插摵闁糕斁鍋撳銈嗗笒閸嬪棝寮ㄩ悧鍫㈢闁告侗鍠楃粈鍫ユ煃瑜滈崜鐔奉焽瑜旀俊鍫曞川婵犱胶绠氶梺鍓插亝濞叉牕顪冮挊澹濆綊鏁愰崨顔兼殘闂佸搫妫崹璺侯潖缂佹ɑ濯撮柣鐔煎亰閸ゅ绱撴担鍓插剱闁搞劌娼￠獮鍡欎沪閸撗呮嚌闂侀€炲苯澧寸€规洘妞介崺鈧い鎺嶉檷娴滄粓鏌熼崫鍕棞濞存粎鍋撶换婵嬫濞戞ǚ鍋撻悷閭︽綎闁煎鍊曢崹婵囥亜閹捐泛鏋庣紒鍓佸仱閺岀喖鏌囬敃鈧晶顔锯偓娈垮枛閸㈡煡鈥旈崘顔嘉ч柛鈩冪懃椤呯磽娴ｇ瓔鍤欑紒澶婎嚟閸欏懎顪冮妶鍡楃瑨閻庢凹鍙冮幃锟犳偄閸忚偐鍘撻梺鍛婄箓鐎氼剟鍩€椤掑倻甯涚紒鏃傚枛瀹曠喖顢橀悩纰夌床闂佸搫顦悧鍕礉鎼淬劌绠犻柛銉㈡杹閸嬫挸鈻撻崹顔界亖闂佸憡姊归崹鍧楀Υ娴ｇ硶妲堟俊顖炴敱椤秴鈹戦埥鍡楃仩闁告艾顑呴弳鈺佲攽閻樺灚鏆╁┑顔诲嵆瀹曡绺界粙鎸庢К闂佸搫璇炵仦鎯у厞闂備線娼ц墝闁哄懏绋撴竟鏇熺節濮橆厾鍘搁悗骞垮劚閸燁偅淇婄捄銊х＜闁绘ê纾幊鍥煛瀹€瀣瘈鐎规洘甯掗埥澶婎潨閸℃鐤傞梻鍌欒兌椤牓鏁冮鍛洸闁割偅娲栭拑鐔兼煥閻斿搫孝缂佲偓閸愵喗鐓忓┑鐐茬仢閸旀挳鏌￠崱鎰伈婵﹨娅ｉ幏鐘诲箵閹烘梹袨闂備焦鎮堕崝蹇撯枖濞戙垺鍋╃€瑰嫭澹嬮弸搴ㄦ煙閹冩毐闁兼澘鐏濋埞鎴炲箠闁稿﹥鎸剧划鍫熺瑹閳ь剟宕洪埀顒併亜閹哄棗浜鹃悗瑙勬处閸撶喖宕洪妷锕€绶為柟閭﹀墰椤旀帒顪冮妶鍡欏闁活収鍠楃粩鐔煎即閵忊檧鎷绘繛杈剧到閹碱偊宕濋敃鍌涚厸闁告侗浜濆▍濠勨偓瑙勬礃濠㈡鐏冮梺鍛婁緱閸犳牗绂嶉鐐粹拺闁圭娴风粻鎾淬亜閿斿灝宓嗙€?
		float existing = reservedDamage.getOrDefault(entityId, 0.0f);
		float totalReserved = existing + damage;
		
		reservedDamage.put(entityId, totalReserved);
		reservationTime.put(entityId, gameTime);

		// 闂傚倸鍊峰ù鍥х暦閸偅鍙忕€规洖娲﹂浠嬫煏閸繃澶勬い顐ｆ礋閺岋繝宕堕妷銉т痪闂佺顑傞弲婵堟閹惧瓨濯撮柣褏顭堥崣鏇犵磽娴ｇ懓鏁剧紓宥勭窔瀵鈽夐姀鐘插祮闂侀潧顭堥崕杈╃不閻熼偊娓婚柕鍫濇閻撱儵鏌ㄩ弴銊ら偗鐎殿喛顕ч埥澶婎潨閸℃ê鍏婃俊鐐€栫敮濠囨倿閿曗偓閻ｇ兘宕樺ù瀣杸闂佺粯鍔樼亸娆愭櫠濞戙垺鐓曢柕濞垮劤娴犮垽鏌ｉ敐鍛Щ妞ゎ厹鍔戝畷鐓庘攽閸繂绠伴梻鍌欑窔閳ь剛鍋涢懟顖涙櫠閹殿喚纾奸弶鍫氭櫅娴犺鲸顨ラ悙宸剶闁轰礁鍊块幐濠冨緞婢跺瞼澶勯梻鍌氬€烽懗鍓佸垝椤栨娑㈠礃椤旇偐鍝楅梻渚囧墮缁夋挳宕ヨぐ鎺撶厵闁绘垶锕╁▓鏇㈡煟?
		float remainingHealth = currentHealth - totalReserved;
		return remainingHealth;
	}

	/**
	 * 闂傚倸鍊峰ù鍥敋瑜忛幑銏ゅ箛椤旇棄搴婇梺鐟邦嚟婵潧鐣烽弻銉︾厱闁斥晛鍟伴埊鏇㈡煕鎼粹槄鏀婚柕鍥у瀵粙濡歌閳ь剚甯￠弻宥囩磼濡椿妫冮梺鍝勭潤閸℃瑧鏉搁梺鍝勬川閸犲骸鈻撶仦鍓х閻庣數顭堝瓭缂備胶绮崝娆撳春閻愬搫绠ｉ柣鎰皺缁愮偤鏌ｆ惔顖滅У闁稿甯掕灋闁告劦鐓堝〒濠氭煏閸繄绠崇€光偓濞戙垺鐓涢柛娑卞枤缁犵偤鏌熼妤€浜炬俊鐐€曠换鎰版偋婵犲洤鐤炬繝濠傜墛閻撶喖鏌熸导瀛樻锭濠⒀傚嵆閺岋絽鈽夐弽褍濮﹀┑顔硷攻濡炶棄鐣烽锕€绀嬫い鎾愁槶閸ㄨ櫣鎹㈠☉姘珰鐟滃繘宕氭导瀛樼厓閻熸瑥瀚悘鎾煕閳规儳浜炬俊鐐€栫敮鎺斺偓姘煎弮瀹曟劙鎮介崨濠勫幈闂佹寧妫侀褍顕ｆィ鍐╃厱闁哄啠鍋撶紒顔兼捣濡叉劙骞掗幘瀵哥Ф闂侀潧臎閸曨偅鐝繝鐢靛剳缁茶棄煤閵堝鏅濇い蹇撳閺嗭箓鏌熼悜妯虹亶婵℃彃鐗婇幈銊ヮ潨閸℃ぞ绨婚梺鎼炲€濇禍璺侯潖缂佹ɑ濯撮柛婵嗗婵箓姊洪崫鍕櫝闁哄懐濮撮悾鐑藉级鐠囩偓妫冨畷銊╊敊闂傛潙鍤遍梻鍌欑劍鐎笛呮崲閸岀偛绠悗锝庡枛閻ゎ噣鏌涘☉妯兼憼闁绘挾濮撮埞鎴﹀磼濮橆剦妫岄梺鍝勵槺閸庛倗鎹㈠☉銏″亗閹艰揪绲介悡鐔兼⒑閸濆嫭婀扮紒瀣墱缁鈽夐姀鐘电潉闂侀€炲苯澧寸€殿喗鎮傚畷鐔碱敇閻樼绱叉俊鐐€栧ú鏍箠鎼淬垺娅犻柟缁㈠枟閻撴瑦銇勯弮鍌涙珪闁瑰啿娲﹂妵鍕閿涘嫭鍣梺鍝ュТ閿曘儱顭囪箛娑樜╅柨鏃傛櫕瑜扮喎鈹戦悩娈挎毌婵℃彃鎳樺畷鎴﹀川椤撗呭數濠碘槅鍨伴惃鐑藉磻閹炬剚娼╅柣鎾冲閻忔捇姊洪柅鐐茶嫰婢у弶銇勯銏╂Ч缂佺粯绋掔换婵嬪炊瑜庨悗顒勬倵楠炲灝鍔氭繛璇х到閳讳粙顢旈崼鐔哄弮濠碘槅鍨拃锕€危閸濆娊褰掓偐濞嗗繑澶勯柣鎾寸☉椤法鎹勯悜姗嗘！濠电偛鎳庡Λ婵嬪蓟閿涘嫪娌悹鍥ㄥ絻椤牓鏌ｉ幘鍗炩偓婵嬪蓟閳╁啫绶炲┑鐘插€婚敍鎲€se闂?
	 * @param entityId 闂傚倸鍊搁崐鐑芥嚄閸洖纾块柣銏㈩焾閻ら箖鏌嶉崫鍕櫣缂佹劖顨嗘穱濠囧Χ閸涱喖娅ら梺绋款儏椤︾敻寮婚妸銉㈡斀闁糕剝顭囬ˇ閬嶆⒑缁嬫鍎愰柟鐟版搐閻ｇ兘鎮滅粵瀣櫍闂佺粯鍔栨竟鍡涙煢閻㈢數纾介柛灞捐壘閳ь剙鎽滅划鏃堝箻椤旇偐鍘存繝?	 * @param damage 婵犵數濮烽。钘壩ｉ崨鏉戠；闁告侗鍙庨悢鍡樹繆椤栨氨姣為柛瀣尭椤繈鎮℃惔锝勬婵＄偑鍊戦崹娲晝閵忕姷鏆﹂柕濠忓缁♀偓闂佺琚崐鏇炩枔婵傚憡鈷掑〒姘ｅ亾婵炰匠鍥ㄥ亱闁糕剝绋戠粻顖炴倵閿濆骸鏋涢柤?
	 * @param currentHealth 闂傚倸鍊搁崐鐑芥嚄閸洖纾块柣銏㈩焾閻ら箖鏌嶉崫鍕櫣缂佹劖顨嗘穱濠囧Χ閸涱喖娅ら梺绋款儏椤︾敻寮婚妸銉㈡斀闁糕剝鐟ラ。鑸电箾鐎涙鐭婄紓宥咃躬瀵鎮㈤悡搴ｇ暰閻熸粌绉瑰铏綇閵婏絼绨婚梺闈涚墕閹冲繘宕甸崶顒佺厸鐎光偓鐎ｎ剛袦婵犳鍠掗崑鎾绘⒑闂堟稓绠冲┑顔炬暬閹绻濆顓涙嫼闂侀潻瀵岄崣搴ㄥ礉濠婂懐纾肩紓浣诡焽缁犳捇鏌?
	 * @param gameTime 闂傚倷娴囧畷鐢稿窗閹邦喖鍨濋幖娣灪濞呯姵淇婇妶鍛櫣缂佺姳鍗抽弻娑樷槈濮楀牊鏁惧┑鐐叉噽婵炩偓闁哄矉绲借灒闁惧繗顫夐崟楣冩⒑闁偛鑻晶顖炴偨椤栨せ鍋撳畷鍥ㄦ闂侀潧顭俊鍥╁姬閳ь剟姊虹粙鎸庢拱缂侇喖鐗忛懞杈ㄧ鐎ｎ偀鎷绘繛杈剧到閹诧繝骞嗛崼銉︾厱闁绘洑绀佹禍浼存煙椤斿搫鍔﹂柟顕嗙節閻?
	 * @return 婵犵數濮烽弫鍛婃叏閻戝鈧倹绂掔€ｎ亞鍔﹀銈嗗坊閸嬫捇鏌涢悢閿嬪仴闁糕斁鍋撳銈嗗坊閸嬫挾绱撳鍜冭含妤犵偛鍟灒閻犲洩灏欑粣鐐烘⒑瑜版帒浜伴柛鎾寸洴椤㈡ê煤椤忓應鎷洪悷婊呭鐢帗绂嶆导瀛樼厱闁规儳顕幊鍥煥濠靛牆浠遍柟顔规櫊瀹曪綁宕掑☉姘垱閻庢鍠栭悥鐓庣暦瑜版帩鏁嬮柛娑卞墰绾惧姊绘担绛嬪殭婵炲鍏橀妴浣圭節閸屾鐎洪梺鍝勬川閸庢劙鎮為挊澶嗘斀闁绘ê鐤囨竟妯讳繆椤愶綇鑰块柡宀€鍠撶划娆撳箰鎼淬垹闂俊鐐€х€靛苯鈻嶉ˇ姒癳闂傚倸鍊搁崐鐑芥倿閿旈敮鍋撶粭娑樻噽閻瑩鏌熸潏楣冩闁搞倖鍔栭妵鍕冀椤愵澀鏉梺閫炲苯澧柟铏崌钘濋柛锔诲幐閸嬫挸鈻撻崹顔界亾濡炪値鍘奸悧鎾诲灳閿曞倸惟闁宠桨绀佺粣娑橆渻閵堝棙鐓ュ褑妫勯悾鐑藉醇閺囩啿鎷绘繛杈剧到閹诧繝骞嗛崼銏㈢＜濠㈣泛顑嗙紞鎴︽偂閵堝鐓忛柛顐ｇ箥濡插摜绱掗埀顒勫礋椤栨稓鍙嗗┑鐐村灦閿氭い蹇ｄ邯閺屾稓鈧綆鍋嗘晶顒勬煏閸パ冾伃妤犵偛娲崺鈩冩媴鏉炵増鍋呴梺璇叉捣閺佸憡鏅跺Δ鍛闁归棿鐒︾粻鎺楁⒒娴ｈ櫣甯涢柨姘辩棯缂併垹骞楅柡鍛埣閺屽棗顓奸崱娆忓箞闂佽鍑界紞鍡涘磻閸℃ɑ娅犳い鎺戝€荤壕濂告偣閸パ冪骇闁搞倕娲ㄩ埀顒侇問閸ｎ噣宕抽敐鍛殾濠靛倸澹婇弫鍐煥濠靛棙锛嶉柛姘懅缁辨捇宕掑顑藉亾閻戣姤鍊块柨鏇炲€哥粻顖涚節闂堟稓澧㈠☉鎾崇Ч閺岀喐娼忛崜褏鏆犻梺鍛婁亢椤濡甸崟顖氬嵆婵°倐鍋撳ù婊堢畺閹鈻撻崹顔界亪闂佺粯鐗曢妶绋跨暦濞差亜鐒垫い鎺嶉檷娴滄粓鏌熼崫鍕ら柛鏂跨Т闇夋繝褍鐏濋埀顒佺箞瀵鏁撻悩鑼紲濠电偞鍨堕敃鈺呭吹鐎ｎ剛纾藉ù锝嗗絻娴滈箖姊洪柅鐐叉噺閸庢lse
	 */
	public boolean tryReserveDamage(int entityId, float damage, float currentHealth, long gameTime) {
		// 濠电姷鏁告慨鐑姐€傞挊澹╋綁宕ㄩ弶鎴狅紱闂侀€炲苯澧撮柡灞剧〒閳ь剨缍嗛崑鍛暦瀹€鍕厸鐎光偓鐎ｎ剛锛熸繛瀵稿婵″洭骞忛悩璇茬闁圭儤鍩堝銉モ攽閻樻鏆柍褜鍓欓崯璺ㄧ棯瑜旈弻鐔碱敊閻撳簶鍋撻幖浣瑰仼闁绘垼妫勫敮闂佸啿鎼崐鐟扳枍閸ヮ剚鈷戦梺顐ゅ仜閼活垱鏅剁€电硶鍋撶憴鍕闁荤啿鏅犲顐﹀箻缂佹ê浜归梺鍦帛鐢偠銇愭径濞炬斀闁绘﹩鍠栭悘閬嶆煕閳哄倻澧电€规洘绻堥弫鍐磼濮橆叀绶㈤梻浣告惈濞层劍鎱ㄦ搴☆棜?
		float existingReservation = reservedDamage.getOrDefault(entityId, 0.0f);
		float existingRemainingHealth = currentHealth - existingReservation;
		
		// 婵犵數濮烽弫鍛婃叏閻戝鈧倹绂掔€ｎ亞鍔﹀銈嗗坊閸嬫捇鏌涢悢閿嬪仴闁糕斁鍋撳銈嗗坊閸嬫挾绱撳鍜冭含妤犵偛鍟灒閻犲洩灏欑粣鐐寸節閻㈤潧孝濡ょ姵鎮傚鏌ユ偐缂佹ǚ鎷洪梺鍛婄☉閿曪箓骞婇崘鈹夸簻闁挎棁妫勯ˉ瀣磼椤旂⒈鐓兼鐐村浮楠炲﹪鎼归銈嗗垱閻庢鍠栭悥鐓庣暦瑜版帩鏁嬮柛娑卞墰绾惧姊绘担绛嬪殭婵炲鍏橀獮濠囧箻鐠轰警妫滃銈嗘尪閸ㄥ綊鎮為崹顐犱簻闁硅揪绲剧涵鍓佺磼娴ｇ妲绘い顓炴健閹兘骞嶉灏栧徍闁诲氦顫夊ú鎴犲緤婵犳鍥亹閹烘挾鍘靛銈嗙墬閼归箖顢旈鐔翠簻闁靛骏绱曢幊鍥煙椤旂厧鈷斿ù鐙呯畵閹墽浠﹂懖鈺冾槰"濠电姷鏁告慨鐢割敊閺嶎厼绐楁俊銈呭暞瀹曟煡鏌涢埄鍐х繁闁轰礁锕ら埞鎴︽偐閹绘帗鐏撳┑?闂傚倸鍊搁崐鐑芥倿閿旈敮鍋撶粭娑樻噽閻瑩鏌熸潏楣冩闁搞倖鍔栭妵鍕冀椤愵澀绮堕梺鎼炲妼閸婂綊濡甸崟顖氬唨闁靛ě浣插亾閹烘鐓冪紓浣股戦埛鎺楁煃瑜滈崜娆戠不瀹ュ纾块梺顒€绉甸崑瀣煕閳╁啰鈽夐柛灞诲姂閺屸剝寰勭€ｎ亝顔曞┑鐐村灟閸ㄥ湱鐥閺屾盯鈥﹂幋婵囩彯婵炴潙鐨烽崑鎾绘⒒?
		if (existingRemainingHealth <= 0) {
			return false;
		}
		
		// 婵犵數濮烽。钘壩ｉ崨鏉戠；闁告侗鍙庨悢鍡樹繆椤栨氨姣為柛瀣尭椤繈鎮℃惔銏壕闂備線鈧偛鑻晶鍙夈亜椤愩埄妲圭紒缁樼⊕缁绘繈宕堕埡浣恒偊闂佽鍑界紞鍡涘窗閺嶎偆涓嶆繛鎴炵懀娴滄粓鏌熼崫鍕棞濞存粍鍎抽—?
		reservedDamage.put(entityId, damage);
		reservationTime.put(entityId, gameTime);
		return true;
	}

	/**
	 * 闂傚倸鍊搁崐椋庣矆娓氣偓瀹曘儳鈧綆鍠栫壕鍧楁煙閹増顥夐幖鏉戯躬閺屻倝鎳濋幍顔肩墯婵炲瓨绮岀紞濠囧蓟濞戙垹唯妞ゆ梻鍘ч～顏堟⒑缁嬪潡顎楅悗娑掓櫊婵＄敻宕熼姘辩潉闂佺鏈粙鎺楁偟椤忓牊鈷戦柟鑲╁仜婵偓闂佺顑嗛崝娆撳春閳ь剚銇勯幒鎴濃偓褰掑吹閳ь剟姊虹涵鍜佸殝缂佺粯绻堥獮鍐晸閻樿櫕娅㈤梺缁橈供閸犳帡宕戦幘璇茬＜闁绘劘灏欓崢鎼佹⒑閸涘﹤濮﹀ù婊勭箞閺佸秷绠涘☉娆屾嫼闂佸湱顭堢€涒晠鍩涢幒妤佺厱閻庯綆鍋呭畷宀勬煙椤曗偓缁犳牕鐣烽敓鐘冲€堕悹鍥у级鐠愶紕绱掗鍛籍鐎规洦鍋婂畷鐔煎Ω閵夈倕顥氶梻浣告惈濞层垽宕洪崟顖氭瀬?
	 * @param entityId 闂傚倸鍊搁崐鐑芥嚄閸洖纾块柣銏㈩焾閻ら箖鏌嶉崫鍕櫣缂佹劖顨嗘穱濠囧Χ閸涱喖娅ら梺绋款儏椤︾敻寮婚妸銉㈡斀闁糕剝顭囬ˇ閬嶆⒑缁嬫鍎愰柟鐟版搐閻ｇ兘鎮滅粵瀣櫍闂佺粯鍔栨竟鍡涙煢閻㈢數纾介柛灞捐壘閳ь剙鎽滅划鏃堝箻椤旇偐鍘存繝?	 * @return 闂傚倷娴囬褍霉閻戣棄绠犻柟鎹愵嚙缁犵喖鏌ㄩ悢鍝勑ｉ柛瀣€块弻銊╂偄閸濆嫅鐐烘煃瑜滈崜娆撳磹閸ф鏄ラ柨鐔哄Т缁€鍐┿亜韫囨挻瀚呯紒鎲嬬節濮婄粯鎷呴悷閭﹀殝濠殿喖锕ら…宄扮暦閹达箑绠荤紓浣姑禒濂告⒑閸涘娈樺ù鍏肩箞瀹曠兘顢橀悩鐢垫殽闂備礁鎲＄换鍌溾偓姘€鍥х；闁规崘顕ч崡鎶芥煏韫囧﹥娅嗛柡鍛櫆缁绘繈鎮介棃娴躲垺绻涚仦鍌氣偓妤佺珶?
	 */
	public float getReservedDamage(int entityId) {
		return reservedDamage.getOrDefault(entityId, 0.0f);
	}

	/**
	 * 闂傚倸鍊搁崐椋庣矆娓氣偓楠炲鏁撻悩鍐蹭画闂佹寧娲栭崐褰掑磻鐎ｎ偂绻嗛柕鍫濇噹閺嗙喖鏌ｉ鐔稿磳婵﹥妞藉畷褰掝敋閸涱厼澹堟俊鐐€栧ú鐔哥閸洖钃熼柡鍥ュ灩闁卞洦绻濋棃娑欐悙妞ゅ孩鐩娲传閵夈儰绮堕梺鍦拡閸嬪鎮樼€ｎ喗鈷戦柛婵嗗濡叉悂鏌ｈ箛鏃傜疄妞ゃ垺妫冮、鏇㈡晜閽樺鍋撻悽鍛婂仭婵炲棗绻愰鈺呮煃缂佹ɑ绀嬮柡灞稿墲閹峰懘宕妷鎰屽應鍋撶憴鍕闁靛牆鎲￠幈銊╁焵椤掑嫭鐓ユ繛鎴灻顐も偓瑙勬礀閻栫厧顫忕紒妯诲闁告稑锕ラ崕鎾剁磽娴ｅ壊妲规繛鍏肩懅閸欏懘姊洪幐搴ｇ畵闁瑰啿顦嵄闁割偆鍠嗘禍婊堢叓閸ャ劍灏伴柛锝呮贡缁辨帡宕滄担闀愭閻庢鍠栭悥鐓庣暦閻撳簶妲堟俊顖欒閻庢娊姊绘担绛嬪殭缂佺粯鍨块妴鍐幢濞戞鐤勯梺闈涱焾閸庮噣寮ㄩ挊澶嗘斀闁绘ɑ鐓￠妤呮煛娴ｈ棄袚濞ｅ洤锕俊鍫曞炊椤噯缍侀弻鐔碱敊閻撳簶鍋撻幖渚囨晪闁挎繂顦介弫瀣煃瑜滈崜鐔兼偘椤曗偓瀹曞崬鈽夊Ο纰卞悑婵＄偑鍊栧濠氭偤閺冨牆鐤柍褜鍓熷濠氬磼濞嗘垵濡介梺娲讳簻缂嶅﹤鐣峰ú顏勭妞ゆ牗绋戞禍妤佺節閵忥絾纭鹃柡鍫墴閿?
	 * @param entityId 闂傚倸鍊搁崐鐑芥嚄閸洖纾块柣銏㈩焾閻ら箖鏌嶉崫鍕櫣缂佹劖顨嗘穱濠囧Χ閸涱喖娅ら梺绋款儏椤︾敻寮婚妸銉㈡斀闁糕剝顭囬ˇ閬嶆⒑缁嬫鍎愰柟鐟版搐閻ｇ兘鎮滅粵瀣櫍闂佺粯鍔栨竟鍡涙煢閻㈢數纾介柛灞捐壘閳ь剙鎽滅划鏃堝箻椤旇偐鍘存繝?	 */
	public void cancelReservation(int entityId) {
		reservedDamage.remove(entityId);
		reservationTime.remove(entityId);
	}

	/**
	 * 缂傚倸鍊搁崐鐑芥嚄閸洘鎯為幖娣妼閻骞栧ǎ顒€濡肩紒鎰殕缁绘盯骞嬪▎蹇曞姶闂佽桨绀佸ú銊ф閹惧瓨濯村┑顔藉焾娴滎亪銆佸鑸电劶鐎广儱妫涢崢顏堟⒑閸撴彃浜濈紒璇插暞閸掑﹥绺介崨濠勫幈闂佺粯鍔橀崺鏍敂閻樼粯鐓冪憸婊堝礈濞戙垹绠犻柟鎹愵嚙缁犵喖鏌ㄩ悢鍝勑ｉ柛瀣€块弻銊╂偄閸濆嫅銏ゆ煟閹烘垹浠涢柕鍥у楠炴帡骞嬪┑鍐ㄤ壕闁告稑锕︽晶锟犳⒒閸屾瑧鍔嶉柟顔肩埣瀹曟繂顓奸崨顖涙畷闂佸綊妫跨粈渚€寮告担琛″亾楠炲灝鍔氭い锔诲灦閺屽宕堕浣哄幗闂佸搫鍊圭€笛囧疮閻愬瓨鍙忛柨婵嗘嚇閸欏嫭鎱ㄦ繝鍐┿仢鐎规洏鍔嶇换婵嬪磼濠娾偓缁辨ɑ淇婇悙顏勨偓鏍р枖閿曞倸鐐婄憸蹇涙偪閸ヮ剚鈷戦柡鍌樺劜濞呭懘鏌涢悢璺哄祮闁糕斂鍨藉顕€鍩€椤掑嫬桅闁告洦鍨扮粻濠氭偣妤︽寧銆冩繛宀婁邯濮婅櫣绮欓崠鈥冲闂佽桨绀侀…鐑界嵁閸愵喖鎹舵い鎾寸☉娴滈箖鏌ㄥ┑鍡欏嚬缂併劌顭烽弻锟犲幢濞嗗繑鐏堥梺鍝勫閸撴繈骞忛崨瀛橆棃婵炴垶甯掓禍鐐節闂堟侗鍎忛柡瀣╄兌閳ь剙绠嶉崕閬嶅箠閹版澘姹查柛鈩冪⊕閻撴洟鏌￠崶銉ュ濞存粍绻堥弻锝呪攽閸喐姣堝┑顔硷龚濞咃綁骞冩导鏉戠伋闁稿繐鎳庨ˉ姘節瀵伴攱婢橀埀顑懎绶ゅù鐘差儏缁愭鎱ㄥΟ鎸庣【婵鐓￠弻锟犲炊閳轰絿銉х棯閹佸仮闁哄瞼鍠栭獮鍡氼槼闁搞倐鍋撴俊鐐€栭幑浣糕枍閿濆洦顫?
	 * @param entityId 闂傚倸鍊搁崐鐑芥嚄閸洖纾块柣銏㈩焾閻ら箖鏌嶉崫鍕櫣缂佹劖顨嗘穱濠囧Χ閸涱喖娅ら梺绋款儏椤︾敻寮婚妸銉㈡斀闁糕剝顭囬ˇ閬嶆⒑缁嬫鍎愰柟鐟版搐閻ｇ兘鎮滅粵瀣櫍闂佺粯鍔栨竟鍡涙煢閻㈢數纾介柛灞捐壘閳ь剙鎽滅划鏃堝箻椤旇偐鍘存繝?	 * @param damage 闂傚倸鍊峰ù鍥敋瑜庨〃銉х矙閸柭も偓鍧楁⒑椤掆偓缁夊澹曟繝姘厪闁割偅绻冩刊濂告煟鎼淬倕鐓愰柕鍥у瀵粙顢曢～顓熷媰闂備胶绮敮鎺椻€﹂悜钘夎摕闁挎繂鎲橀悢鐓庝紶闁告洦鍓氶惁锝夋⒒娴ｇ瓔鍤欑紒缁樺姇鐓ゆい鎾卞灩閽冪喖鏌ㄥ┑鍡╂Ц閹喖姊洪棃娑辨Ф闁稿海鍎ょ粋鎺楁偡閹佃櫕鏂€闂佺粯蓱椤旀牠寮抽鐐寸厱閻庯綆鍋呭畷宀勬煛?
	 */
	public void confirmDamage(int entityId, float damage) {
		float reserved = reservedDamage.getOrDefault(entityId, 0.0f);
		if (reserved <= damage) {
			// 婵犵數濮烽。钘壩ｉ崨鏉戠；闁告侗鍙庨悢鍡樹繆椤栨氨姣為柛瀣尭椤繈鎮℃惔銏壕闂備線鈧偛鑻晶鍙夈亜椤愩埄妲圭紒缁樼⊕缁绘繈宕惰閻ｉ箖姊洪崨濠傚Е闁哥姵鐗滅划鍫⑩偓锝庡亖娴滄粓鐓崶銊﹀鞍闁革絼绮欓幃褰掑箛椤斿吋鐏堥梺鍝勭焿缁查箖骞嗛弮鍫熸櫜闁糕剝顨嗛悾顒勬⒒娴ｅ憡鎲搁柛鐘查椤洭鍨惧畷鍥ㄦ濠电偛妫欓幐濠氬疾閹间焦鐓熸俊顖氱仢閻ㄧ儤銇勮熁閸ャ劉鎷洪梻鍌氱墛閸楁洟宕奸妷銉ф煣濠电偞鍨崹鍦矆婢舵劖鐓忓璺烘濞呭棝鏌嶉柨瀣仸闁靛洤瀚伴獮鍥煛娴ｆ彃浜鹃柡鍥╁枔婢э繝姊婚崒娆戝妽闁诡喖鐖煎畷婵嗩吋閸涱垱娈曢梺褰掓？缁€渚€寮告担琛″亾楠炲灝鍔氭繛璇х到閳讳粙顢旈崼鐔哄幈闂佸湱鍋撻〃鍛附閺冨倹顫曢柍鍝勬噺閳锋帡鏌涚仦鎹愬闁逞屽墮閹芥粓鍩€椤掍礁鎼搁柛鏃€鍨甸悾鐑藉箛椤戣姤鏂€闁诲函绲婚崝澶愬磻閹捐纾奸柣鎰皺閸樻悂姊洪崨濠傚Е濞存粍绻堥弫?
			reservedDamage.remove(entityId);
			reservationTime.remove(entityId);
		} else {
			// 闂傚倸鍊搁崐鎼佸磹妞嬪孩顐介柨鐔哄Т缁愭鏌熼幑鎰靛殭缂佲偓婢跺备鍋撻崗澶婁壕闂佸憡娲﹂崑鍕倵椤撱垺鐓欓柛蹇氬亹閺嗘﹢鏌涢妸銉хШ妞ゃ垺宀搁弻鍡楊吋閸℃瑥甯鹃梻浣稿閸嬪懐鎹㈤崘鈺佸灁濠靛倸鎲￠悡鏇㈡煟閹邦剛鎽犻柕鍥ㄧ箖閹便劍绻濋崘鈹夸虎濡炪們鍨哄ú鐔镐繆閸洖宸濋柛婊冨暟缁夘噣鏌＄仦鍓ф创濠碘剝鎮傞弫鍐焵椤掑嫬浼犳繛宸簼閻撴稑霉閿濆懏鎲搁懖鏍ㄧ箾鐎电顎撶紒鐘虫崌楠炲啫鈻庨幙鍐╂櫌闂佺鏈懝鎯掗幇鐗堚拻闁稿本鐟чˇ锕傛煙绾板崬浜扮€规洘鍔欓幃鐑芥焽閿旀儳鏁告繝纰樻閸ㄥ磭鍒掗姘ｆ瀺闁绘绮悡娆戠磼鐎ｎ亞浠㈡い鎺嬪灩閳规垿顢涘杈ㄦ喖缂備胶绮惄顖氱暦閸楃倣鐔虹磼濡搫澹嗛梻鍌欑閹碱偄螞濞嗗緷鍝勵煥閸涱噮娼?
			reservedDamage.put(entityId, reserved - damage);
		}
	}

	/**
	 * 濠电姷鏁告慨鐑藉极閹间礁纾婚柣鎰惈缁犱即鏌熼梻瀵割槮缂佺姷濞€閺岀喖鎮ч崼鐔哄嚒缂備胶濮甸悧鏇㈠煘閹达附鍋愰柟缁樺笚濞堝弶绻涚€涙ê娈犻柛濠冪箞瀵鈽夐姀鐘栥劑鏌ㄥ┑鍡樺櫣妞ゎ剙顦靛铏瑰寲閺囩喐鐝旈梺绋匡工濞尖€愁嚕婵犳碍鏅插璺好￠埡鍛厪濠㈣泛鐗嗛崝鏉懨归悩鍨殌闁宠鍨块幃鈺佲枔閹稿孩鐦滈梻浣侯焾椤戝棝骞戦崶顒€钃熼柨婵嗘閸庣喖鏌曢崼婵嗩劉缂傚秴鐗婄换婵嬫偨闂堟稐绮堕梺鎸庢处娴滎亝淇婇幘顔肩＜闁绘劘灏欓崢鎼佹⒑閸涘﹤濮﹀ù婊勭箞閺?
	 * @param currentTime 闂傚倷娴囧畷鐢稿窗閹邦喖鍨濋幖娣灪濞呯姵淇婇妶鍛櫣缂佺姳鍗抽弻娑樷槈濮楀牊鏁惧┑鐐叉噽婵炩偓闁哄矉绲借灒闁惧繗顫夐崟楣冩⒑闁偛鑻晶顖炴偨椤栨せ鍋撳畷鍥ㄦ闂侀潧顭俊鍥╁姬閳ь剟姊虹粙鎸庢拱缂侇喖鐗忛懞杈ㄧ鐎ｎ偀鎷绘繛杈剧到閹诧繝骞嗛崼銉︾厱闁绘洑绀佹禍浼存煙椤斿搫鍔﹂柟顕嗙節閻?
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
	 * 濠电姷鏁告慨鐑姐€傞挊澹╋綁宕ㄩ弶鎴狅紱闂侀€炲苯澧撮柡灞剧〒閳ь剨缍嗛崑鍛暦瀹€鍕厸鐎光偓鐎ｎ剛锛熸繛瀵稿婵″洭骞忛悩璇茬闁圭儤鍤╅幋鐐电瘈闁汇垽娼ф禒鈺冣偓娈垮枟閹瑰洤鐣烽敓鐘茬鐎瑰壊鍠栧▓銊︾箾鐎电孝妞ゆ垵鎳橀幃娆愮節閸ャ劎鍘繝鐢靛亼閸婃劙宕戦幘缁樼叆闁哄洨鍋涢埀顒€缍婇幃锟犲即閻旂寮垮┑鈽嗗灠閹碱偊鍩涢弮鍫熺厽闁挎繂顦幉楣冩煛鐏炵晫啸妞ぱ傜窔閺屾盯骞樼€靛憡鍒涢悗瑙勬磸閸ㄥジ藝鐎涙绠鹃柛鈩冨姇閻忔煡鏌＄仦鑺ヮ棞妞ゆ挸銈稿畷銊╊敍濮橆厽鍟熼梻鍌欐祰瀹曞灚鎱ㄩ弶鎳ㄦ椽鏁冩担鍏告睏闂佸憡鍔戦崜閬嶅箳閹惧墎鐦堥梺鎼炲劘閸斿酣宕ｉ崱妞绘斀闁绘劖娼欓悘锔姐亜椤撶偞澶勯柡鍛埣瀵挳濮€閳ュ厖缂撴俊鐐€栭悧妤呫€冮崨顔绢洸濡わ絽鍟埛鎴︽煠婵劕鈧洘绂掗敂鍓х＜閻庯綆鍋勯悘鎾煙椤栨碍婀伴柟宄版嚇閺屽懎鈽夊杈╂毎濠碉紕鍋戦崐鏍箰閻愵剚鍙忕€瑰嫭瀚堝☉銏╂晬闁绘劕顕崣鍡涙⒑閸濆嫭宸濋柛瀣洴閸┾偓妞ゆ帒鍊归弳顒傗偓瑙勬礃閸ㄥ潡鐛Ο鍏煎珰闁圭粯甯掗幐銈夋⒒婵犲骸浜滄繛璇х畱鐓ら柡宥庡亞閸楁艾鈹戦崒姘暈闁?
	 * @param entityId 闂傚倸鍊搁崐鐑芥嚄閸洖纾块柣銏㈩焾閻ら箖鏌嶉崫鍕櫣缂佹劖顨嗘穱濠囧Χ閸涱喖娅ら梺绋款儏椤︾敻寮婚妸銉㈡斀闁糕剝顭囬ˇ閬嶆⒑缁嬫鍎愰柟鐟版搐閻ｇ兘鎮滅粵瀣櫍闂佺粯鍔栨竟鍡涙煢閻㈢數纾介柛灞捐壘閳ь剙鎽滅划鏃堝箻椤旇偐鍘存繝?	 * @param currentHealth 闂傚倸鍊搁崐鐑芥嚄閸洖纾块柣銏㈩焾閻ら箖鏌嶉崫鍕櫣缂佹劖顨嗘穱濠囧Χ閸涱喖娅ら梺绋款儏椤︾敻寮婚妸銉㈡斀闁糕剝鐟ラ。鑸电箾鐎涙鐭婄紓宥咃躬瀵鎮㈤悡搴ｇ暰閻熸粌绉瑰铏綇閵婏絼绨婚梺闈涚墕閹冲繘宕甸崶顒佺厸鐎光偓鐎ｎ剛袦婵犳鍠掗崑鎾绘⒑闂堟稓绠冲┑顔炬暬閹绻濆顓涙嫼闂侀潻瀵岄崣搴ㄥ礉濠婂懐纾肩紓浣诡焽缁犳捇鏌?
	 * @return 婵犵數濮烽弫鍛婃叏閻戝鈧倹绂掔€ｎ亞鍔﹀銈嗗坊閸嬫捇鏌涢悢閿嬪仴闁糕斁鍋撳銈嗗坊閸嬫挾绱撳鍜冭含妤犵偛鍟灒閻犲洩灏欑粣鐐烘⒑瑜版帒浜伴柛妯恒偢瀹曟粓顢欑喊杈ㄥ瘜闂侀潧鐗嗗Λ娆撳煕閹烘鐓涢柛婊€绀佹禍鎵偓瑙勬礀缂嶅﹪骞冮敓鐘靛祦闁绘棃顥撶粔娲煙椤旇娅婇柟宕囧█椤㈡牠顢曢悩娴嬪亾濠靛钃熸繛鎴欏灩濡﹢鏌ｉ幇顒€绾ч柟鍏煎姉閻ヮ亪骞嗚缁夋椽鏌″畝瀣М妤犵偞锕㈤幖褰掝敃閿濆懘妫风紓鍌氬€峰ù鍥敊婵犲嫭顐芥慨妯挎硾閽冪喖鏌ㄩ悢鍝勑㈤柣鎺戠仛閵囧嫰骞掗幋婵冨亾閸︻厸鍋撳顓炲摵闁哄瞼鍠撶槐鎺懳熼搹鍦嚃闂備椒绱梽鍕偋閻樿钃熼柍鈺佸暙缁剁偤鏌涢埄鍏╂垿鎮甸弴銏♀拺闁告繂瀚～锕傛煕閺傝法鐏遍柛娆忔嚇濮婃椽宕崟鍨枔闂佸搫鎳愮挧纾?	 */
	public boolean isTargetWorthAttacking(int entityId, float currentHealth) {
		if (!isThriftyMode()) {
			return true; // Reservation logic only applies while thrifty mode is active.
		}
		
		float reserved = getReservedDamage(entityId);
		float remainingHealth = currentHealth - reserved;
		
		// If reserved damage already covers the remaining health, skip the target.
		return remainingHealth > 0;
	}

	private void checkCreativePowerComponent() {
        if (hasCreativePowerComponent() && level != null && !level.isClientSide) {
            setEnergyFull();
        }
    }
    
    /**
     * 闂傚倸鍊搁崐鐑芥嚄閸洖纾块柣銏㈩焾閻ょ偓绻涢幋娆忕仾闁稿鍊濋弻鏇熺箾瑜嶇€氼厼鈻撴导瀛樼厽閹兼番鍔嶅☉褔鏌熷ù瀣⒉闁告帒锕ョ缓浠嬪川婵犲嫬骞堥梻浣告贡閸嬫捇銆冮崨鏉戠疇闊洦绋掗崐鍨亜閹捐泛浜归柡鈧禒瀣厽婵☆垳鍎ら埢鏇㈡煟閹垮啫骞樼紒杈ㄥ笚濞煎繘濡搁敂缁㈡Ч闁诲孩顔栭崳顕€宕戞繝鍥╁祦闁哄秲鍔嶆刊鎾偡濞嗗繐顏柣婵愬亰濮婄粯鎷呯粙娆炬闂佺顑勭欢姘暦閻熸噴娲敂閸曨剝绶㈤梻濠庡亜濞诧箓骞忕€ｎ喗鍋￠梺顓ㄥ閸欏棝姊洪崨濠傚Е闁革綆鍣ｉ崺鈧い鎺嶇贰濞堟粓鏌＄仦鍓ф创濠碘剝鎮傞崺锟犲焵椤掑嫬绠熷Δ锝呭暞閻撴盯鎮橀悙璺盒撻柛銈呮喘閺岀喖顢欑憴鍕彋閻庤娲﹂崑濠傜暦閻旂⒈鏁冮柕蹇嬪焺閸氬懘姊婚崒姘偓鎼佸磹閻戣姤鍊块柨鏇氶檷娴滃綊鏌涢幇鍏哥凹鐎规洘鐓￠弻鐔兼倻濡崵鍙嗛梺鎼炲妽缁诲嫰鍩€椤掆偓缁犲秹宕曢崡鐐嶆稑鈽夐～顑藉亾閸涘瓨鍊婚柤鎭掑劚娴狀垱绻涙潏鍓у埌闁硅绱曢幏褰掓晬閸曨厾锛滈梺?
     */
    private void setEnergyFull() {
        if (energyStorage.getEnergyStored() < energyStorage.getMaxEnergyStored()) {
            energyStorage.setEnergyStored(energyStorage.getMaxEnergyStored());
            setChanged();
            syncToClient();
        }
    }
    
    /**
     * 闂傚倸鍊搁崐鐑芥嚄閸洖纾块柣銏㈩焾閻ょ偓绻涢幋娆忕仾闁稿鍊濋弻鏇熺箾瑜嶇€氼厼鈻撴导瀛樼厽閹兼番鍨婚埊鏇㈡嫅鏉堛劎绠鹃柛娑卞灠閳诲牓鏌″畝瀣М妤犵偛娲、姗€鎮欓悽鐐瑰仭濠碉紕鍋戦崐銈夊磻閹惧墎绀婂〒姘ｅ亾鐎殿喖顭锋俊鑸靛緞婵犲嫷妲梻浣告啞娓氭宕归幎鑺ュ亗闁瑰墽绮埛鎴︽⒒閸碍娅囩紒瀣帛娣囧﹪鎮欓幍顔剧厯閻庢鍣崑鍕敇閸忕厧绶炲┑鐘叉搐閺佸綊姊绘笟鈧褏鎹㈤崼銉ュ瀭婵炲樊浜滅粻鏍ㄦ叏濡炶浜鹃梺鍝勫閸撴繂顕ラ崟顖氬耿婵☆垵宕佃ぐ鍛節绾版ǚ鍋撻崘娴嬫寖闂佽娴烽摂濠籩ceive闂傚倸鍊搁崐鎼佸磹閹间礁纾归柛婵勫劗閸嬫挸顫濋悡搴＄睄閻庤娲樼换鍫濐嚕閹绢喖顫呴柣妯挎珪琚ｅ┑鐘茬棄閺夊簱鍋撻弴銏犵疇闊洦绋戦悿?
     * @param amount 婵犵數濮烽弫鍛婃叏椤撱垹绠柛鎰靛枛瀹告繃銇勯幘瀵哥畼闁硅娲栭埞鎴︽偐閹颁礁鏅遍梺鍝ュУ瀹€绋跨暦閹达箑围濠㈣泛锕﹂悾楣冩⒑閸涘﹤濮﹂柛鐘崇墱缁牏鈧綆鍋佹禍婊堟煛閸愩劌鈧摜鏁懜娈挎闁绘劕妯婇崕鏃€鎱ㄦ繝鍕笡闁瑰嘲鎳橀幃鐑藉箥椤斾勘鍋＄紓鍌氬€烽懗鍓佸垝椤栫偛绀夋繛鍡樻尭閺勩儵鏌?
     * @return 闂傚倸鍊峰ù鍥敋瑜庨〃銉х矙閸柭も偓鍧楁⒑椤掆偓缁夊澹曟繝姘厪闁割偅绻冩刊濂告煟鎼淬倕鐓愰柕鍥у瀵粙濡歌婵洤鈹戦悙鍙夊櫧濠电偐鍋撻梺鍝勭焿缂嶄線鐛崶顒夋晣闁绘劗鏁搁妶閿嬩繆閻愵亜鈧倝宕戦幘鍓佺濞撴埃鍋撶€殿喖顭烽弫鎰緞婵炩懇鏅犻弻鏇熷緞閸繂濮舵繛瀵稿帶閸婂灝顫忛搹鍦煓閻犳亽鍔庨澶愭⒑閹稿孩纾搁柛銊ㄦ椤?
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
    
    // ========== Mounted Turret Types ==========
    
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
    
    // ========== Mounted Turret Types ==========
    
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
        
        // 闂傚倸鍊搁崐椋庣矆娴ｉ潻鑰块弶鍫氭櫅閸ㄦ繃銇勯弽銊х煁闁哄棙绮岄埞鎴︽偐閹绘巻鍋撻悽绋垮嚑閹兼番鍔嶉悡蹇涚叓閸パ屽剰闁逞屽墯閻楃娀骞冮敓鐘茬劦妞ゆ帒瀚崐鍨殽閻愯尙浠㈤柛鏃€纰嶇换娑氫沪閸屾艾顫囬悗娈垮枟閹倸鐣烽幒妤佸€烽悗鐢殿焾楠炴绻濋悽闈浶㈤柨鏇樺€楅埀顒佸嚬閸樻儳鈻庨姀銈呯煑濠㈣泛鐬奸惁鍫ユ⒑闁偛鑻晶瀛樸亜閵忊剝顥堢€规洝绮剧粻娑㈠箻閹绘帩妫滃┑鐘垫暩閸嬬偤宕归崼鏇ㄦ晜妞ゅ繐鐗滈弫鍌炴煕椤愶絿绠橀柨娑欑洴閹鐛崹顔煎濡炪倧瀵岄崹鍫曠嵁?PluginSlot 闂傚倸鍊搁崐鐑芥倿閿曞倹鍎戠憸鐗堝笒缁€澶屸偓鍏夊亾闁逞屽墴閸┾偓妞ゆ帊绀侀崵顒勬煕閻樻剚娈滅€殿喗鐓″畷濂稿即閻愮儤鏆呮繝寰锋澘鈧劙宕戦幘瓒?NBT 婵犵數濮烽弫鎼佸磻閻愬搫鍨傞柛顐ｆ礀缁犲綊鏌嶉崫鍕櫣闁活厽顨婇悡顐﹀炊閵婏妇顦ㄥ銈庡亝濞叉鎹㈠┑瀣棃婵炴垶鐟ョ粣娑㈡⒑瀹曞洨甯涙慨濠傤煼閸┾偓妞ゆ巻鍋撶紒鐘茬Ч瀹曟洟鏌嗗畵銉ユ喘閹囧醇閵忊晜绁繝娈垮枟閵囨盯宕戦幘缁樼厵妞ゆ梹鍎抽崢瀵糕偓娈垮枟濞兼瑩锝炲┑鍥舵綑闁哄秲鍓卞鍫熺厽閹兼番鍊ゅ鎰箾閸欏鐭掔€规洑鍗冲浠嬵敇閻愮數鏆俊鐐€曠换鎰版偋濠婂牆鏋侀柛鏇ㄥ灡閸嬶綁鏌熼鐔风瑨濠碘剝瀵ч妵鍕籍閳ь剙螞閸愵喖钃?
        
        if (owner != null) {
            tag.putUUID("Owner", owner);
        }
        String cachedOwnerName = getCachedOwnerName();
        if (!cachedOwnerName.isEmpty()) {
            tag.putString("OwnerName", cachedOwnerName);
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
        if (tag.contains("OwnerName")) {
            ownerName = tag.getString("OwnerName");
        } else {
            ownerName = "";
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
        // Range upgrades increase the base radius by 1 per installed component.
        double upgradedRadius = baseRadius + count;
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
    
    // ========== Mounted Turret Types ==========
    
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
     * 闂傚倸鍊搁崐椋庣矆娓氣偓楠炲鏁撻悩鍐叉疄婵°倧绲介崯顐も偓姘槹閵囧嫰骞掗幋婵愪紝闂佽桨绀佸Λ婵嬪蓟閿濆绠涙い鎺嶇劍閸庢捇姊洪崫鍕靛剮缂佽埖宀稿濠氭晲閸涘倹妫冮崺鈧い鎺戝閸嬪鏌涢埄鍐噮闁活厼鐗撻弻銊╁即閻愭祴鍋撹ぐ鎺撳亗闁哄洢鍨洪悡娆撴煟閹寸倖鎴犱焊閹殿喚纾奸悹鍝勬惈缁狙囨煏閸パ冾伃妤犵偞锚閻ｇ兘宕堕埞顑惧妼铻栭柣姗€娼ф禒婊勩亜閹存繃顥㈤挊婵嬫煟濡も偓閻楀﹪宕曢悢鎼炰簻闁哄啫娲ら崥鍦棯?     */
    public void syncToClient() {
        if (level != null && !level.isClientSide) {
            level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
        }
    }

    public void dropStoredItems(Level level, BlockPos pos) {
        dropHandlerItems(level, pos, ammoInventory);
        dropHandlerItems(level, pos, basePluginSlot);
        for (ItemStackHandler faceUpgradeSlot : faceUpgradeSlots) {
            dropHandlerItems(level, pos, faceUpgradeSlot);
        }
    }

    private void dropHandlerItems(Level level, BlockPos pos, ItemStackHandler handler) {
        for (int slot = 0; slot < handler.getSlots(); slot++) {
            ItemStack stack = handler.getStackInSlot(slot);
            if (stack.isEmpty()) {
                continue;
            }
            Containers.dropItemStack(level, pos.getX(), pos.getY(), pos.getZ(), stack.copy());
            handler.setStackInSlot(slot, ItemStack.EMPTY);
        }
    }
    
    // ========== Mounted Turret Types ==========
    
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