package com.xuebi1145.xuplus_client.mixin;

import com.phasetranscrystal.blockoffensive.client.data.CSClientData;
import com.phasetranscrystal.blockoffensive.client.screen.CSGameShopScreen;
import com.phasetranscrystal.blockoffensive.map.shop.ItemType;
import com.phasetranscrystal.fpsmatch.common.client.FPSMClient;
import com.phasetranscrystal.fpsmatch.common.client.shop.ClientShopSlot;
import com.phasetranscrystal.fpsmatch.util.RenderUtil;
import icyllis.modernui.core.Context;
import icyllis.modernui.fragment.Fragment;
import icyllis.modernui.graphics.Canvas;
import icyllis.modernui.graphics.Image;
import icyllis.modernui.graphics.drawable.ImageDrawable;
import icyllis.modernui.graphics.drawable.ShapeDrawable;
import icyllis.modernui.util.DataSet;
import icyllis.modernui.text.Typeface;
import icyllis.modernui.view.Gravity;
import icyllis.modernui.view.LayoutInflater;
import icyllis.modernui.view.MeasureSpec;
import icyllis.modernui.view.View;
import icyllis.modernui.view.ViewGroup;
import icyllis.modernui.widget.ImageView;
import icyllis.modernui.widget.LinearLayout;
import icyllis.modernui.widget.RelativeLayout;
import icyllis.modernui.widget.TextView;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;

import java.util.ArrayList;
import java.util.List;

import static icyllis.modernui.view.ViewGroup.LayoutParams.MATCH_PARENT;
import static icyllis.modernui.view.ViewGroup.LayoutParams.WRAP_CONTENT;

@Mixin(value = CSGameShopScreen.class, remap = false)
public abstract class MixinCSGameShopScreen extends Fragment {

    private static final String[] TOP_NAME_KEYS = new String[]{
        "blockoffensive.shop.title.equipment",
        "blockoffensive.shop.title.pistol",
        "blockoffensive.shop.title.mid_rank",
        "blockoffensive.shop.title.rifle",
        "blockoffensive.shop.title.throwable"
    };

    /**
     * @author OpenAI
     * @reason 使用盒子布局重做商店界面，并保留原有商店逻辑与按钮实现。
     */
    @Overwrite
    public View onCreateView(@NotNull LayoutInflater inflater, ViewGroup container, DataSet savedInstanceState) {
        Context context = container != null ? container.getContext() : getContext();
        return new XuPlusShopLayout(context);
    }

    private static final class XuPlusShopLayout extends RelativeLayout {
        private final ImageView background;
        private final RelativeLayout rootBox;
        private final RelativeLayout topBarBox;
        private final LinearLayout categoryRow;
        private final LinearLayout categoryContentRow;
        private final RelativeLayout previewBox;
        private final TextView moneyText;
        private final TextView cooldownText;
        private final TextView minMoneyText;
        private final TextView previewTitle;
        private final TextView previewMeta;
        private final TextView previewDesc;
        private final List<CSGameShopScreen.TypeBarLayout> bars = new ArrayList<>();
        private float scale = 1.0F;

        private XuPlusShopLayout(Context context) {
            super(context);
            setLayoutParams(new ViewGroup.LayoutParams(MATCH_PARENT, MATCH_PARENT));

            background = new ImageView(context);
            ImageDrawable backgroundDrawable = new ImageDrawable(Image.create("xuplus_client", "textures/cs2/shop.png"));
            backgroundDrawable.setAlpha(52);
            background.setImageDrawable(backgroundDrawable);
            background.setScaleType(ImageView.ScaleType.FIT_XY);
            addView(background, new RelativeLayout.LayoutParams(MATCH_PARENT, MATCH_PARENT));

            rootBox = new RelativeLayout(context);
            rootBox.setBackground(rect(RenderUtil.color(16, 19, 24, 230), RenderUtil.color(44, 50, 58, 255), 6));
            addView(rootBox);

            topBarBox = new RelativeLayout(context);
            topBarBox.setBackground(rect(RenderUtil.color(29, 34, 41, 230), RenderUtil.color(56, 65, 74, 255), 4));
            rootBox.addView(topBarBox);

            moneyText = text(context, 22, RenderUtil.color(240, 208, 95), true);
            cooldownText = text(context, 14, RenderUtil.color(220, 224, 230), false);
            minMoneyText = text(context, 14, RenderUtil.color(160, 166, 174), false);
            topBarBox.addView(moneyText);
            topBarBox.addView(cooldownText);
            topBarBox.addView(minMoneyText);

            categoryRow = new LinearLayout(context);
            categoryRow.setOrientation(LinearLayout.HORIZONTAL);
            rootBox.addView(categoryRow);

            categoryContentRow = new LinearLayout(context);
            categoryContentRow.setOrientation(LinearLayout.HORIZONTAL);
            rootBox.addView(categoryContentRow);

            for (int i = 0; i < 5; i++) {
                TextView title = text(context, 15, RenderUtil.color(215, 219, 223), false);
                title.setText(I18n.get(TOP_NAME_KEYS[i]));
                title.setGravity(Gravity.CENTER);
                categoryRow.addView(title);

                CSGameShopScreen.TypeBarLayout bar = new CSGameShopScreen.TypeBarLayout(context, i);
                bar.setBackground(rect(RenderUtil.color(18, 21, 27, 208), RenderUtil.color(54, 61, 69, 255), 4));
                bar.setScale(0.92F);
                bars.add(bar);
                categoryContentRow.addView(bar);
            }

            previewBox = new RelativeLayout(context);
            previewBox.setBackground(rect(RenderUtil.color(23, 27, 33, 232), RenderUtil.color(74, 84, 96, 255), 5));
            rootBox.addView(previewBox);

            previewTitle = text(context, 22, RenderUtil.color(244, 247, 250), true);
            previewMeta = text(context, 15, RenderUtil.color(173, 181, 189), false);
            previewDesc = text(context, 15, RenderUtil.color(202, 208, 214), false);
            previewDesc.setGravity(Gravity.TOP | Gravity.LEFT);

            previewBox.addView(previewTitle);
            previewBox.addView(previewMeta);
            previewBox.addView(previewDesc);
        }

        @Override
        protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
            int width = MeasureSpec.getSize(widthMeasureSpec);
            int height = MeasureSpec.getSize(heightMeasureSpec);
            setMeasuredDimension(width, height);

            scale = Math.min(width / 1920.0F, height / 1080.0F);
            scale = Math.max(0.62F, Math.min(0.90F, scale));

            int rootW = Math.round(1280 * scale);
            int rootH = Math.round(730 * scale);
            int topBarH = Math.round(52 * scale);
            int categoryTitleH = Math.round(36 * scale);
            int gridH = Math.round(548 * scale);
            int previewW = Math.round(250 * scale);
            int previewH = Math.round(548 * scale);

            background.measure(MeasureSpec.makeMeasureSpec(width, MeasureSpec.EXACTLY), MeasureSpec.makeMeasureSpec(height, MeasureSpec.EXACTLY));
            rootBox.measure(MeasureSpec.makeMeasureSpec(rootW, MeasureSpec.EXACTLY), MeasureSpec.makeMeasureSpec(rootH, MeasureSpec.EXACTLY));
            topBarBox.measure(MeasureSpec.makeMeasureSpec(rootW - Math.round(32 * scale), MeasureSpec.EXACTLY), MeasureSpec.makeMeasureSpec(topBarH, MeasureSpec.EXACTLY));
            categoryRow.measure(MeasureSpec.makeMeasureSpec(rootW - previewW - Math.round(54 * scale), MeasureSpec.EXACTLY), MeasureSpec.makeMeasureSpec(categoryTitleH, MeasureSpec.EXACTLY));
            categoryContentRow.measure(MeasureSpec.makeMeasureSpec(rootW - previewW - Math.round(54 * scale), MeasureSpec.EXACTLY), MeasureSpec.makeMeasureSpec(gridH, MeasureSpec.EXACTLY));
            previewBox.measure(MeasureSpec.makeMeasureSpec(previewW, MeasureSpec.EXACTLY), MeasureSpec.makeMeasureSpec(previewH, MeasureSpec.EXACTLY));

            int[] widths = scaledWidths();
            for (int i = 0; i < categoryRow.getChildCount(); i++) {
                View title = categoryRow.getChildAt(i);
                title.measure(MeasureSpec.makeMeasureSpec(widths[i], MeasureSpec.EXACTLY), MeasureSpec.makeMeasureSpec(categoryTitleH, MeasureSpec.EXACTLY));

                CSGameShopScreen.TypeBarLayout bar = bars.get(i);
                bar.setScale(scale * 0.92F);
                bar.measure(MeasureSpec.makeMeasureSpec(widths[i], MeasureSpec.EXACTLY), MeasureSpec.makeMeasureSpec(gridH, MeasureSpec.EXACTLY));
            }

            previewTitle.measure(MeasureSpec.makeMeasureSpec(previewW - Math.round(32 * scale), MeasureSpec.EXACTLY), MeasureSpec.makeMeasureSpec(Math.round(34 * scale), MeasureSpec.EXACTLY));
            previewMeta.measure(MeasureSpec.makeMeasureSpec(previewW - Math.round(32 * scale), MeasureSpec.EXACTLY), MeasureSpec.makeMeasureSpec(Math.round(92 * scale), MeasureSpec.EXACTLY));
            previewDesc.measure(MeasureSpec.makeMeasureSpec(previewW - Math.round(32 * scale), MeasureSpec.EXACTLY), MeasureSpec.makeMeasureSpec(previewH - Math.round(176 * scale), MeasureSpec.EXACTLY));
        }

        @Override
        protected void onLayout(boolean changed, int left, int top, int right, int bottom) {
            int width = right - left;
            int height = bottom - top;
            int rootW = rootBox.getMeasuredWidth();
            int rootH = rootBox.getMeasuredHeight();
            int rootX = (width - rootW) / 2;
            int rootY = (height - rootH) / 2;

            background.layout(0, 0, width, height);
            rootBox.layout(rootX, rootY, rootX + rootW, rootY + rootH);

            int inset = Math.round(16 * scale);
            int topBarX = inset;
            int topBarY = inset;
            topBarBox.layout(topBarX, topBarY, topBarX + topBarBox.getMeasuredWidth(), topBarY + topBarBox.getMeasuredHeight());

            int categoryTitleY = topBarBox.getBottom() + Math.round(16 * scale);
            categoryRow.layout(topBarX, categoryTitleY, topBarX + categoryRow.getMeasuredWidth(), categoryTitleY + categoryRow.getMeasuredHeight());

            int categoryContentY = categoryRow.getBottom() + Math.round(10 * scale);
            categoryContentRow.layout(topBarX, categoryContentY, topBarX + categoryContentRow.getMeasuredWidth(), categoryContentY + categoryContentRow.getMeasuredHeight());

            int previewX = rootW - inset - previewBox.getMeasuredWidth();
            previewBox.layout(previewX, categoryContentY, previewX + previewBox.getMeasuredWidth(), categoryContentY + previewBox.getMeasuredHeight());

            int[] widths = scaledWidths();
            int gap = Math.round(12 * scale);
            int pieceX = 0;
            for (int i = 0; i < categoryRow.getChildCount(); i++) {
                View title = categoryRow.getChildAt(i);
                title.layout(pieceX, 0, pieceX + widths[i], categoryRow.getMeasuredHeight());

                View bar = categoryContentRow.getChildAt(i);
                bar.layout(pieceX, 0, pieceX + widths[i], categoryContentRow.getMeasuredHeight());
                pieceX += widths[i] + gap;
            }

            moneyText.layout(Math.round(16 * scale), Math.round(7 * scale), Math.round(220 * scale), topBarBox.getMeasuredHeight());
            cooldownText.layout(Math.round(340 * scale), Math.round(9 * scale), Math.round(620 * scale), topBarBox.getMeasuredHeight());
            minMoneyText.layout(topBarBox.getMeasuredWidth() - Math.round(250 * scale), Math.round(9 * scale), topBarBox.getMeasuredWidth() - Math.round(14 * scale), topBarBox.getMeasuredHeight());

            previewTitle.layout(Math.round(16 * scale), Math.round(14 * scale), previewBox.getMeasuredWidth() - Math.round(16 * scale), Math.round(44 * scale));
            previewMeta.layout(Math.round(16 * scale), Math.round(52 * scale), previewBox.getMeasuredWidth() - Math.round(16 * scale), Math.round(140 * scale));
            previewDesc.layout(Math.round(16 * scale), Math.round(150 * scale), previewBox.getMeasuredWidth() - Math.round(16 * scale), previewBox.getMeasuredHeight() - Math.round(16 * scale));
        }

        @Override
        public void draw(@NotNull Canvas canvas) {
            super.draw(canvas);
            updateTopBar();
            updatePreview();
        }

        private int[] scaledWidths() {
            int[] base = new int[]{170, 170, 170, 210, 170};
            int[] scaled = new int[base.length];
            for (int i = 0; i < base.length; i++) {
                scaled[i] = Math.round(base[i] * scale);
            }
            return scaled;
        }

        private void updateTopBar() {
            int money = CSClientData.getMoney() == -1 ? 16000 : CSClientData.getMoney();
            moneyText.setText("$ " + money);
            moneyText.setTextColor(FPSMClient.getGlobalData().isCurrentTeam("ct") ? RenderUtil.color(150, 200, 250) : RenderUtil.color(234, 192, 85));
            cooldownText.setText(I18n.get("blockoffensive.shop.title.cooldown", CSClientData.shopCloseTime));
            int nextRoundMinMoney = CSClientData.getNextRoundMinMoney();
            minMoneyText.setText(nextRoundMinMoney >= 0 ? I18n.get("blockoffensive.shop.title.min.money", nextRoundMinMoney) : "");
        }

        private void updatePreview() {
            CSGameShopScreen.GunButtonLayout focused = null;
            for (ItemType type : ItemType.values()) {
                List<CSGameShopScreen.GunButtonLayout> list = CSGameShopScreen.shopButtons.get(type);
                if (list == null) {
                    continue;
                }
                for (CSGameShopScreen.GunButtonLayout entry : list) {
                    if (entry.isHovered()) {
                        focused = entry;
                        break;
                    }
                }
                if (focused != null) {
                    break;
                }
            }

            if (focused == null) {
                previewTitle.setText("");
                previewMeta.setText("");
                previewDesc.setText("");
                return;
            }

            ClientShopSlot slot = focused.getSlot();
            ItemStack stack = slot.itemStack();
            String name = stack.isEmpty() ? I18n.get("blockoffensive.shop.slot.empty") : slot.name();
            previewTitle.setText(name);
            previewMeta.setText("价格  $" + slot.cost() + "\n分类  " + I18n.get(TOP_NAME_KEYS[focused.type.ordinal()]));
            previewDesc.setText(
                "可退货  " + (slot.canReturn() ? "是" : "否") +
                    "\n已购买  " + slot.boughtCount() +
                    "\n是否锁定  " + (slot.isLocked() ? "是" : "否") +
                    "\n当前阵营  " + FPSMClient.getGlobalData().getCurrentTeam()
            );
        }

        private static ShapeDrawable rect(int fillColor, int strokeColor, int radius) {
            ShapeDrawable drawable = new ShapeDrawable();
            drawable.setShape(ShapeDrawable.RECTANGLE);
            drawable.setColor(fillColor);
            drawable.setCornerRadius(radius);
            drawable.setStroke(1, strokeColor);
            return drawable;
        }

        private static TextView text(Context context, int size, int color, boolean bold) {
            TextView text = new TextView(context);
            text.setTextSize(size);
            text.setTextColor(color);
            return text;
        }
    }
}
