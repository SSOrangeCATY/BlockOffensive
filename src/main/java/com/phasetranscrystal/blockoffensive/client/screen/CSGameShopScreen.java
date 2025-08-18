package com.phasetranscrystal.blockoffensive.client.screen;

import com.phasetranscrystal.blockoffensive.BlockOffensive;
import com.phasetranscrystal.blockoffensive.client.data.CSClientData;
import com.phasetranscrystal.blockoffensive.client.renderer.ShopSlotRenderer;
import com.phasetranscrystal.blockoffensive.map.shop.ItemType;
import com.phasetranscrystal.fpsmatch.common.client.FPSMClient;
import com.phasetranscrystal.fpsmatch.common.client.shop.ClientShopSlot;
import com.phasetranscrystal.fpsmatch.common.packet.register.NetworkPacketRegister;
import com.phasetranscrystal.fpsmatch.common.packet.shop.ShopActionC2SPacket;
import com.phasetranscrystal.fpsmatch.core.shop.ShopAction;
import com.phasetranscrystal.fpsmatch.util.RenderUtil;
import com.tacz.guns.api.TimelessAPI;
import com.tacz.guns.client.resource.GunDisplayInstance;
import icyllis.modernui.animation.TimeInterpolator;
import icyllis.modernui.animation.ValueAnimator;
import icyllis.modernui.core.Context;
import icyllis.modernui.fragment.Fragment;
import icyllis.modernui.graphics.Canvas;
import icyllis.modernui.graphics.Image;
import icyllis.modernui.graphics.drawable.ImageDrawable;
import icyllis.modernui.graphics.drawable.ShapeDrawable;
import icyllis.modernui.mc.MinecraftSurfaceView;
import icyllis.modernui.mc.ScreenCallback;
import icyllis.modernui.util.DataSet;
import icyllis.modernui.view.Gravity;
import icyllis.modernui.view.LayoutInflater;
import icyllis.modernui.view.View;
import icyllis.modernui.view.ViewGroup;
import icyllis.modernui.widget.ImageView;
import icyllis.modernui.widget.LinearLayout;
import icyllis.modernui.widget.RelativeLayout;
import icyllis.modernui.widget.TextView;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.NotNull;

import java.util.*;

import static icyllis.modernui.view.ViewGroup.LayoutParams.WRAP_CONTENT;


public class CSGameShopScreen extends Fragment implements ScreenCallback{
    public static final Map<ItemType, List<GunButtonLayout>> shopButtons = new HashMap<>();
    public static final String BACKGROUND = "ui/cs/background.png";
    public static final int DISABLE_TEXT_COLOR = RenderUtil.color(100,100,100);
    private static final String[] TOP_NAME_KEYS = new String[]{"blockoffensive.shop.title.equipment","blockoffensive.shop.title.pistol","blockoffensive.shop.title.mid_rank","blockoffensive.shop.title.rifle","blockoffensive.shop.title.throwable"};
    public static boolean refreshFlag = false;
    private static CSGameShopScreen INSTANCE;

    public CSGameShopScreen(){
    }

    public static float calculateScaleFactor(int w, int h) {
        return Math.min((float) w / 1920,(float) h / 1080);
    }

    public static CSGameShopScreen getInstance() {
        if(INSTANCE == null) {
            INSTANCE = new CSGameShopScreen();
        }
        return INSTANCE;
    }

    public View onCreateView(@NotNull LayoutInflater inflater, ViewGroup container, DataSet savedInstanceState) {
        return new WindowLayout(getContext());
    }

    public static class WindowLayout extends RelativeLayout {
        private float scale = calculateScaleFactor(this.getWidth(),this.getHeight());
        // main
        private ImageView background;
        private RelativeLayout headBar;
        private LinearLayout content;

        // header bar start
        public TextView moneyText;
        public TextView cooldownText;
        public TextView nextRoundMinMoneyText;
        // end
        // ----------------------------------------------------
        // content start
        public LinearLayout shopWindow;
        public List<TypeBarLayout> typeBarLayouts = new ArrayList<>();
        // end

        public WindowLayout(Context context) {
            super(context);
            initializeLayout();
        }

        private void initializeLayout() {
            content = new LinearLayout(getContext());
            content.setOrientation(LinearLayout.HORIZONTAL);
            background = new ImageView(getContext());
            ImageDrawable imageDrawable = new ImageDrawable(Image.create(BlockOffensive.MODID, BACKGROUND));
            imageDrawable.setAlpha(60);

            background.setImageDrawable(imageDrawable);
            background.setScaleType(ImageView.ScaleType.FIT_CENTER);

            shopWindow = new LinearLayout(this.getContext());
            for (int i = 0; i < 5; i++) {
                TypeBarLayout typeBar = new TypeBarLayout(this.getContext(),i);
                shopWindow.addView(typeBar, new LinearLayout.LayoutParams((int) ((TypeBarLayout.getGunButtonWeight(i) + 30) * scale), -1));
                typeBarLayouts.add(typeBar);
            }
            content.addView(shopWindow, new LinearLayout.LayoutParams((int) (950 * scale), (int) (550* scale)));

            LayoutParams shopWindowParams = new LayoutParams(
                    (int) (950 * scale),
                    (int) (550 * scale));
            shopWindowParams.setMargins(0, (int) (210 * scale), 0, 0);
            shopWindowParams.addRule(RelativeLayout.CENTER_HORIZONTAL);
            LayoutParams shopWindowBackGroundParams = new LayoutParams(
                    (int) (950 * scale),
                    (int) (550 * scale));
            shopWindowBackGroundParams.setMargins(0, (int) (208 * scale), 0, 0);
            shopWindowBackGroundParams.addRule(RelativeLayout.CENTER_HORIZONTAL);
            // HEAD BAR START
            headBar = new RelativeLayout(getContext());
            LayoutParams titleBarParams = new LayoutParams((int) (scale *950), (int) (scale*38));
            titleBarParams.setMargins(0, (int) (scale * 190), 0, 0);
            titleBarParams.addRule(RelativeLayout.CENTER_HORIZONTAL);

            ImageView titleBarBackground = new ImageView(getContext());
            titleBarBackground.setImageDrawable(imageDrawable);
            titleBarBackground.setScaleType(ImageView.ScaleType.FIT_XY);
            headBar.addView(titleBarBackground);
            moneyText = new TextView(getContext());
            LayoutParams moneyParams = new LayoutParams(
                    WRAP_CONTENT,
                    WRAP_CONTENT);
            moneyParams.addRule(RelativeLayout.CENTER_IN_PARENT);
            moneyParams.addRule(RelativeLayout.ALIGN_PARENT_LEFT);
            moneyParams.setMargins((int) (25* scale),0,0,0);
            moneyText.setLayoutParams(moneyParams);
            moneyText.setTextColor(FPSMClient.getGlobalData().equalsTeam("ct") ? RenderUtil.color(150,200,250) : RenderUtil.color(234, 192, 85));
            moneyText.setTextSize(18 * scale);

            cooldownText = new TextView(getContext());
            LayoutParams cooldownParams = new LayoutParams(
                    WRAP_CONTENT,
                    WRAP_CONTENT);
            cooldownParams.addRule(RelativeLayout.CENTER_IN_PARENT);
            cooldownText.setText(I18n.get("blockoffensive.shop.title.cooldown",CSClientData.shopCloseTime));
            cooldownText.setLayoutParams(cooldownParams);
            cooldownText.setTextSize(18 * scale);

            nextRoundMinMoneyText = new TextView(getContext());
            LayoutParams minMoneyText = new LayoutParams(
                    WRAP_CONTENT,
                    WRAP_CONTENT);
            minMoneyText.addRule(RelativeLayout.CENTER_IN_PARENT);
            minMoneyText.addRule(RelativeLayout.ALIGN_PARENT_RIGHT);
            nextRoundMinMoneyText.setText(I18n.get("blockoffensive.shop.title.min.money", CSClientData.getNextRoundMinMoney()));
            minMoneyText.setMargins(0,0, (int) (20* scale),0);
            nextRoundMinMoneyText.setLayoutParams(minMoneyText);
            nextRoundMinMoneyText.setTextSize(15 * scale);
            headBar.addView(moneyText);
            headBar.addView(cooldownText);
            headBar.addView(nextRoundMinMoneyText);
            //END

            addView(headBar, titleBarParams);
            addView(background, shopWindowBackGroundParams);
            addView(content, shopWindowParams);
        }

        @Override
        protected void onSizeChanged(int width, int height, int prevWidth, int prevHeight) {
            scale = calculateScaleFactor(width, height);

            LayoutParams shopWindowParams = new LayoutParams(
                    (int) (950 * scale),
                    (int) (550 * scale));
            shopWindowParams.setMargins(0, (int) (210 * scale), 0, 0);
            shopWindowParams.addRule(RelativeLayout.CENTER_HORIZONTAL);

            LayoutParams shopWindowBackGroundParams = new LayoutParams(
                    (int) (950 * scale),
                    (int) (550 * scale));
            shopWindowBackGroundParams.setMargins(0, (int) (208 * scale), 0, 0);
            shopWindowBackGroundParams.addRule(RelativeLayout.CENTER_HORIZONTAL);

            this.content.setLayoutParams(shopWindowParams);
            this.background.setLayoutParams(shopWindowBackGroundParams);

            LayoutParams titleBarParams = new LayoutParams((int) (scale*950), (int) (scale*38));
            titleBarParams.setMargins(0, (int) (scale * 170), 0, 0);
            titleBarParams.addRule(RelativeLayout.CENTER_HORIZONTAL);
            this.headBar.setLayoutParams(titleBarParams);

            LayoutParams minMoneyText = new LayoutParams(
                    WRAP_CONTENT,
                    WRAP_CONTENT);
            minMoneyText.addRule(RelativeLayout.CENTER_IN_PARENT);
            minMoneyText.addRule(RelativeLayout.ALIGN_PARENT_RIGHT);
            minMoneyText.setMargins(0,0, (int) (20 * scale),0);
            nextRoundMinMoneyText.setLayoutParams(minMoneyText);
            nextRoundMinMoneyText.setTextSize(15 * scale);

            LayoutParams moneyParams = new LayoutParams(
                    WRAP_CONTENT,
                    WRAP_CONTENT);
            moneyParams.addRule(RelativeLayout.CENTER_IN_PARENT);
            moneyParams.addRule(RelativeLayout.ALIGN_PARENT_LEFT);
            moneyParams.setMargins((int) (25 * scale),0,0,0);
            moneyText.setLayoutParams(moneyParams);
            moneyText.setTextSize(18 * scale);

            shopWindow.setLayoutParams(new LinearLayout.LayoutParams((int) (950 * scale), (int) (550* scale)));

            cooldownText.setTextSize(18* scale);

            typeBarLayouts.forEach(typeBarLayout -> typeBarLayout.setScale(scale));

            shopButtons.forEach((type,gunButtons)-> gunButtons.forEach(gunButtonLayout -> gunButtonLayout.setScale(scale)));
        }
        @Override
        public void draw(@NotNull Canvas canvas) {
            super.draw(canvas);
            updateText();
        }

        public void updateText(){
            moneyText.setText("$ "+CSClientData.getMoney());
            moneyText.setTextColor(FPSMClient.getGlobalData().equalsTeam("ct") ? RenderUtil.color(150,200,250) : RenderUtil.color(234, 192, 85));
            nextRoundMinMoneyText.setText(I18n.get("blockoffensive.shop.title.min.money", CSClientData.getNextRoundMinMoney()));
            cooldownText.setText(I18n.get("blockoffensive.shop.title.cooldown",CSClientData.shopCloseTime));
        }

    }

    public static class TypeBarLayout extends LinearLayout {
        int i;
        LinearLayout titleBar;
        TextView numTab;
        TextView title;
        List<LinearLayout> guns = new ArrayList<>();
        List<LinearLayout> shops = new ArrayList<>();

        public TypeBarLayout(Context context,int i) {
            super(context);
            this.i = i;
            setOrientation(LinearLayout.VERTICAL);
            titleBar = new LinearLayout(getContext());
            int textColor = RenderUtil.color(203, 203, 203);
            numTab = new TextView(getContext());
            numTab.setTextColor(textColor);
            numTab.setText(String.valueOf(i + 1));
            numTab.setTextSize(15);
            numTab.setPadding(15, 10, 0, 0);
            numTab.setGravity(Gravity.LEFT);

            title = new TextView(getContext());
            title.setTextColor(textColor);
            title.setText(I18n.get(TOP_NAME_KEYS[i]));
            title.setTextSize(21);
            title.setGravity(Gravity.CENTER);

            titleBar.addView(numTab, new LayoutParams((25), -1));
            titleBar.addView(title, new LayoutParams(((getGunButtonWeight(i) - 25)), -1));
            addView(titleBar, new LayoutParams(-1, (44)));
            List<GunButtonLayout> buttons = new ArrayList<>();
            for (int j = 0; j < 5; j++) {
                var shop = new LinearLayout(getContext());
                var gun = new LinearLayout(getContext());
                GunButtonLayout gunButtonLayout = new GunButtonLayout(getContext(), ItemType.values()[i], j);
                buttons.add(gunButtonLayout);
                gun.addView(gunButtonLayout, new LayoutParams(-1, -1));
                guns.add(gun);

                shop.setGravity(Gravity.CENTER);
                shop.addView(gun, new LayoutParams((getGunButtonWeight(i)), (90)));
                shops.add(shop);

                addView(shop, new LayoutParams(-1, (98)));
            }
            // 添加按钮到全局管理 具体缩放逻辑由窗口直接代理
            shopButtons.put(ItemType.values()[i], buttons);
        }

        public static int getGunButtonWeight(int i){
            return switch (i) {
                case 2 -> 180;
                case 3 -> 200;
                default -> 140;
            };
        }

        private void setScale(float scale) {
            numTab.setTextSize(15 * scale);
            numTab.setPadding((int) (15 * scale), (int) (10 * scale), 0, 0);
            numTab.setLayoutParams(new LayoutParams((int) (25 * scale), -1));

            title.setLayoutParams(new LayoutParams((int) ((getGunButtonWeight(i) - 25) * scale), -1));
            title.setTextSize(21 * scale);

            titleBar.setLayoutParams(new LayoutParams(-1, (int) (44 * scale)));

            guns.forEach((gun)-> gun.setLayoutParams(new LayoutParams((int) (getGunButtonWeight(i) * scale), (int) (90 * scale))));

            shops.forEach((shop)-> shop.setLayoutParams(new LayoutParams(-1, (int) (98 * scale))));

            this.setLayoutParams(new LayoutParams((int) ((TypeBarLayout.getGunButtonWeight(i) + 30) * scale), -1));
        }

    }

    public static class GunButtonLayout extends RelativeLayout {
        public final ItemType type;
        public final int index;
        public final ShapeDrawable background;
        public final RelativeLayout returnGoodsLayout;
        public final ValueAnimator backgroundAnimeFadeIn;
        public final ValueAnimator backgroundAnimeFadeOut;
        public final TextView numText;
        public final TextView itemNameText;
        public final TextView costText;
        public final TextView returnGoodsText;
        public MinecraftSurfaceView minecraftSurfaceView;
        public ShopSlotRenderer shopSlotRenderer;

        public GunButtonLayout(Context context, ItemType type, int index) {
            super(context);
            this.type = type;
            this.index = index;

            setGravity(Gravity.CENTER);
            setLayoutParams(new LayoutParams(-1, -1));

            this.background = new ShapeDrawable();
            background.setShape(ShapeDrawable.RECTANGLE);
            background.setColor(RenderUtil.color(42, 42, 42));
            background.setCornerRadius(3);
            background.setAlpha(200);
            setBackground(background);

            minecraftSurfaceView = new MinecraftSurfaceView(getContext());

            ClientShopSlot currentSlot = getSlot();
            Optional<GunDisplayInstance> display = TimelessAPI.getGunDisplay(currentSlot.itemStack());
            LayoutParams msvp;
            if(display.isPresent()) {
                msvp = new LayoutParams(117, 59);
            }else{
                msvp = new LayoutParams(39, 39);
            }
            msvp.addRule(RelativeLayout.CENTER_IN_PARENT);
            minecraftSurfaceView.setLayoutParams(msvp);
            this.shopSlotRenderer = new ShopSlotRenderer(this.type, this.index);
            minecraftSurfaceView.setRenderer(this.shopSlotRenderer);
            addView(minecraftSurfaceView);

            numText = new TextView(getContext());
            numText.setTextSize(13);
            numText.setText(String.valueOf(this.index + 1));
            LayoutParams numParams = new LayoutParams(
                    WRAP_CONTENT,
                    WRAP_CONTENT);
            numParams.addRule(RelativeLayout.ALIGN_PARENT_TOP);
            numParams.addRule(RelativeLayout.ALIGN_PARENT_LEFT);
            numParams.setMargins(5,5,0,0);
            numText.setLayoutParams(numParams);

            itemNameText = new TextView(getContext());
            itemNameText.setTextSize(13);
            itemNameText.setText(this.getSlot().itemStack().isEmpty() ? I18n.get("blockoffensive.shop.slot.empty") : getSlot().name());
            LayoutParams itemNameParams = new LayoutParams(
                    WRAP_CONTENT,
                    WRAP_CONTENT);
            itemNameParams.addRule(RelativeLayout.ALIGN_PARENT_TOP);
            itemNameParams.addRule(RelativeLayout.ALIGN_PARENT_RIGHT);
            itemNameParams.setMargins(0 , 5, 5,0);
            itemNameText.setLayoutParams(itemNameParams);

            returnGoodsText = new TextView(getContext());
            returnGoodsText.setTextSize(15);
            returnGoodsText.setText("↩");
            LayoutParams returnGoodsParams = new LayoutParams(
                    WRAP_CONTENT,
                    WRAP_CONTENT);
            returnGoodsParams.addRule(RelativeLayout.ALIGN_PARENT_BOTTOM);
            returnGoodsParams.addRule(RelativeLayout.ALIGN_PARENT_LEFT);
            returnGoodsParams.setMargins(5,12,0,0);
            returnGoodsText.setLayoutParams(returnGoodsParams);
            returnGoodsLayout = new RelativeLayout(getContext()){
                @Override
                public void setEnabled(boolean enabled) {
                    returnGoodsText.setAlpha(enabled ? 255:0);
                    super.setEnabled(enabled);
                }
            };
            returnGoodsLayout.addView(returnGoodsText);
            returnGoodsLayout.setOnClickListener((l)-> NetworkPacketRegister.getChannelFromCache(ShopActionC2SPacket.class).sendToServer(new ShopActionC2SPacket(FPSMClient.getGlobalData().getCurrentMap(),this.type,this.index, ShopAction.RETURN)));

            returnGoodsLayout.setEnabled(false);
            addView(returnGoodsLayout);

            costText = new TextView(getContext());
            costText.setText("$ "+currentSlot.cost());
            costText.setTextSize(12);
            LayoutParams costParams = new LayoutParams(
                    WRAP_CONTENT,
                    WRAP_CONTENT);
            costParams.addRule(RelativeLayout.ALIGN_PARENT_BOTTOM);
            costParams.addRule(RelativeLayout.ALIGN_PARENT_RIGHT);
            costParams.setMargins(0,0,5,5);
            costText.setLayoutParams(costParams);

            addView(numText);
            addView(itemNameText);
            addView(costText);

            backgroundAnimeFadeIn = ValueAnimator.ofInt(42, 72);
            backgroundAnimeFadeIn.setDuration(200);
            backgroundAnimeFadeIn.setInterpolator(TimeInterpolator.SINE);
            backgroundAnimeFadeIn.addUpdateListener(animation -> {
                int color = (int) animation.getAnimatedValue();
                this.background.setColor(RenderUtil.color(color,color,color));
            });

            backgroundAnimeFadeOut = ValueAnimator.ofInt(72, 42);
            backgroundAnimeFadeOut.setDuration(200);
            backgroundAnimeFadeOut.setInterpolator(TimeInterpolator.SINE);
            backgroundAnimeFadeOut.addUpdateListener(animation -> {
                int color = (int) animation.getAnimatedValue();
                this.background.setColor(RenderUtil.color(color,color,color));
            });

            setOnClickListener((v) -> {
                boolean enable = CSClientData.getMoney() >= currentSlot.cost() && !currentSlot.itemStack().isEmpty() && !currentSlot.isLocked();
                if(enable) NetworkPacketRegister.getChannelFromCache(ShopActionC2SPacket.class).sendToServer(new ShopActionC2SPacket(FPSMClient.getGlobalData().getCurrentMap(), this.type, this.index, ShopAction.BUY));
            });
        }

        public void setStats(boolean enable){
            background.setStroke(enable ? 1:0,RenderUtil.color(255,255,255));
            this.returnGoodsLayout.setEnabled(enable);
        }

        public void setElements(boolean enable){
            ClientShopSlot currentSlot = getSlot();
            if(enable){
                int color = FPSMClient.getGlobalData().equalsTeam("ct") ? RenderUtil.color(150,200,250) : RenderUtil.color(234, 192, 85);
                numText.setTextColor(color);
                itemNameText.setTextColor(color);
                costText.setTextColor(color);
            }else{
                numText.setTextColor(CSGameShopScreen.DISABLE_TEXT_COLOR);
                itemNameText.setTextColor(CSGameShopScreen.DISABLE_TEXT_COLOR);
                costText.setTextColor(CSGameShopScreen.DISABLE_TEXT_COLOR);
            }

            if(currentSlot.boughtCount() > 0){
                background.setStroke(1,RenderUtil.color(255,255,255));
            }else{
                background.setStroke(0,RenderUtil.color(255,255,255));
            }

            returnGoodsLayout.setEnabled(currentSlot.canReturn());
        }

        public ClientShopSlot getSlot(){
            return FPSMClient.getGlobalData().getSlotData(this.type.name(),this.index);
        }

        public void updateButtonState() {
            ClientShopSlot currentSlot = this.getSlot();
            boolean enable = CSClientData.getMoney() >= currentSlot.cost() && !currentSlot.itemStack().isEmpty() && !currentSlot.isLocked();
            this.setElements(enable);

            if(!this.isHovered()) {
                backgroundAnimeFadeIn.start();
            }else{
                backgroundAnimeFadeOut.start();
            }

            if(refreshFlag){
                ClientShopSlot data = getSlot();
                setStats(data.canReturn());
                ItemStack itemStack = data.itemStack();
                boolean empty = itemStack.isEmpty();
                this.itemNameText.setText(empty ? I18n.get("blockoffensive.shop.slot.empty") : data.name());
                this.costText.setText("$ "+ data.cost());
                this.invalidate();

                //END
                if(this.type == ItemType.THROWABLE && this.index == 4){
                    refreshFlag = false;
                }
            }
        }

        private void setScale(float scale) {
            ClientShopSlot currentSlot = getSlot();
            Optional<GunDisplayInstance> display = TimelessAPI.getGunDisplay(currentSlot.itemStack());
            LayoutParams msvp;
            if(display.isPresent()) {
                msvp = new LayoutParams((int) (117 * scale), (int) (59 * scale));
            }else{
                msvp = new LayoutParams((int) (39*scale), (int) (39*scale));
            }
            msvp.addRule(RelativeLayout.CENTER_IN_PARENT);
            minecraftSurfaceView.setLayoutParams(msvp);
            shopSlotRenderer.setScale(scale);

            LayoutParams numParams = new LayoutParams(
                    WRAP_CONTENT,
                    WRAP_CONTENT);
            numParams.addRule(RelativeLayout.ALIGN_PARENT_TOP);
            numParams.addRule(RelativeLayout.ALIGN_PARENT_LEFT);
            numParams.setMargins((int) (5 * scale), (int) (5*scale),0,0);
            numText.setLayoutParams(numParams);
            numText.setTextSize(13*scale);

            LayoutParams itemNameParams = new LayoutParams(
                    WRAP_CONTENT,
                    WRAP_CONTENT);
            itemNameParams.addRule(RelativeLayout.ALIGN_PARENT_TOP);
            itemNameParams.addRule(RelativeLayout.ALIGN_PARENT_RIGHT);
            itemNameParams.setMargins(0 ,(int) (5*scale),(int) (5 * scale),0);
            itemNameText.setLayoutParams(itemNameParams);
            itemNameText.setTextSize(13*scale);

            LayoutParams returnGoodsParams = new LayoutParams(
                    WRAP_CONTENT,
                    WRAP_CONTENT);
            returnGoodsParams.addRule(RelativeLayout.ALIGN_PARENT_BOTTOM);
            returnGoodsParams.addRule(RelativeLayout.ALIGN_PARENT_LEFT);
            returnGoodsParams.setMargins((int) (5* scale), (int) (12*scale),0,0);
            returnGoodsText.setLayoutParams(returnGoodsParams);
            returnGoodsText.setTextSize(15*scale);

            LayoutParams costParams = new LayoutParams(
                    WRAP_CONTENT,
                    WRAP_CONTENT);
            costParams.addRule(RelativeLayout.ALIGN_PARENT_BOTTOM);
            costParams.addRule(RelativeLayout.ALIGN_PARENT_RIGHT);
            costParams.setMargins(0,0, (int) (5*scale), (int) (5*scale));
            costText.setLayoutParams(costParams);
            costText.setTextSize(12*scale);
            this.setLayoutParams(new LinearLayout.LayoutParams((int) (TypeBarLayout.getGunButtonWeight(this.type.ordinal()) * scale), (int) (90 * scale)));
        }
        @Override
        public void draw(@NotNull Canvas canvas) {
            super.draw(canvas);
            updateButtonState();
        }
    }
}
