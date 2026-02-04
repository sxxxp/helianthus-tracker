package com.example;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandManager;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.message.v1.ClientReceiveMessageEvents;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.fabricmc.fabric.api.event.player.AttackBlockCallback;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.TextColor;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

import java.io.InputStream;
import java.io.InputStreamReader;

import java.text.DecimalFormat;
import java.util.Map;
import java.util.Objects;


public class ExampleModClient implements ClientModInitializer {

	private boolean isTracking = false;
	private final Minecraft client = Minecraft.getInstance();
	private static final long PAUSE_THRESHOLD = 1500;
	DecimalFormat df = new DecimalFormat("###,###");
	private final int UNCOMMON = 0x55FF55;
	private final int RARE = 0x5555FF;
	private final int CRAZYRARE = 0xFF55FF;
	private final int PRAYTORNGESUS = 0xAA00AA;
	private final int LEGENDARY = 0xFFAA00;
	private final JsonObject cropFeverData = loadCropFever();
	private String breakItem = "";
	private int uncommonCrop = 0;
	private int rareCrop = 0;
	private int uncommon = 0;
	private int rare = 0;
	private int crazyRare = 0;
	private int prayToRNGESUS = 0;
	private int totalProfit = 0;
	@Override
	public void onInitializeClient() {
		ConfigManager.load();
		ClientReceiveMessageEvents.GAME.register((message, overlay) -> {
			String cleanMessage = message.getString();
			if (cleanMessage.contains("RARE CROP!") && cleanMessage.contains("Helianthus")) {
				if (ConfigManager.config.lastFoundTime != 0) {
					ConfigManager.config.helianthusAmount++;
					if(ConfigManager.config.bestTime>getDiff() || ConfigManager.config.bestTime == 0){
						ConfigManager.config.bestTime = getDiff();
					}
					ConfigManager.save();
					showTimeDiffTitle();
				}

				long currentTime = System.currentTimeMillis();
				ConfigManager.config.blockAmount = 0;
				ConfigManager.config.totalTime += getDiff();
				ConfigManager.config.lastFoundTime = currentTime;
				ConfigManager.config.lastBreakTime = currentTime;
				ConfigManager.config.accumulatedActiveTime = 0;
				ConfigManager.config.startBreakTime = currentTime;

			}
			if (cleanMessage.contains("You dropped")) {
				String[] parts = cleanMessage.split(" DROP! ");
				breakItem = getCrop(parts[1]);
				int color = switch (parts[0]) {
                    case "UNCOMMON" -> {
                        uncommon++;
                        yield UNCOMMON;
                    }
                    case "RARE" -> {
                       	rare++;
                        yield RARE;
                    }
                    case "CRAZY RARE" -> {
						if(parts[1].contains("flower") && parts[1].contains("6")){
							prayToRNGESUS++;
							yield PRAYTORNGESUS;
						}else{
							crazyRare++;
							yield CRAZYRARE;
						}
                    }
                    case "PRAY TO RNGESUS" -> {
                        prayToRNGESUS++;
                        yield PRAYTORNGESUS;
                    }
                    default -> 0xFFFFFF;
                };

				playGradeSound(parts[0]);
			}
			if (cleanMessage.contains("WOAH!") && cleanMessage.contains("CROP FEVER")) {
				ConfigManager.config.cropFeverAmount++;
				ConfigManager.save();
			}else if (cleanMessage.contains("CROP FEVER") && cleanMessage.contains("GONE!")){
				cropFeverResult();
				sendFeverResult();
				resetFeverResult();
			}
		});
		ClientCommandRegistrationCallback.EVENT.register((dispatcher, registryAccess) -> {
			dispatcher.register(ClientCommandManager.literal("htreset")
					.executes(context -> {
						ConfigManager.reset();
						context.getSource().sendFeedback(Component.literal("helianthus tracker value reset success"));
				return 1;
			}));
		});


		AttackBlockCallback.EVENT.register((player, world, hand, pos, direction) -> {
			if (world.isClientSide) {
				BlockState state = world.getBlockState(pos);
				Block block = state.getBlock();
				if (block == Blocks.SUNFLOWER || block == Blocks.ROSE_BUSH) {
					ConfigManager.config.blockAmount++;
					handleCropBreak();
				}
			}
			return InteractionResult.PASS;
		});

		HudRenderCallback.EVENT.register((context,tracker) -> {
			ItemStack item = Objects.requireNonNull(client.player).getMainHandItem();
			String displayName = item.getDisplayName().getString();
			if(displayName.contains("Hoe Mk. I")){
				MutableComponent multiLineComponent = Component.empty();

				multiLineComponent.append(colorComponent(timeConverter(),UNCOMMON)).append("\n");
				multiLineComponent.append(Component.literal( "x" + ConfigManager.config.helianthusAmount)
						).append(colorComponent(" Helianthus",LEGENDARY)).append("\n");

				multiLineComponent.append(colorComponent("AVG TIME: "))
						.append(colorComponent(timeConverter(ConfigManager.config.helianthusAmount == 0 ? 0 : ConfigManager.config.totalTime / ConfigManager.config.helianthusAmount),CRAZYRARE)).append("\n");

				multiLineComponent.append(colorComponent("BEST TIME: "))
						.append(colorComponent(timeConverter(ConfigManager.config.bestTime),LEGENDARY)).append("\n");

				multiLineComponent.append(colorComponent("CROP FEVER: ",CRAZYRARE))
						.append(colorComponent(ConfigManager.config.cropFeverAmount+"")).append("\n");

//				multiLineComponent.append(colorComponent("UNCOMMON DROP: ",UNCOMMON))
//						.append(Component.literal(""+ConfigManager.config.uncommon)).append("\n");
//				multiLineComponent.append(colorComponent("RARE DROP: ",RARE))
//						.append(Component.literal(""+ConfigManager.config.rare)).append("\n");
//				multiLineComponent.append(colorComponent("CRAZY RARE DROP: ",CRAZYRARE))
//						.append(Component.literal(""+ConfigManager.config.crazyRare)).append("\n");
//				multiLineComponent.append(colorComponent("PRAY TO RNGESUS DROP: ",PRAYTORNGESUS))
//						.append(Component.literal(""+ConfigManager.config.prayToRNGESUS)).append("\n");
				context.drawWordWrap(client.font, multiLineComponent, 10, 10,200, 0xFFFFFFFF,true);
			}
			});

		ClientTickEvents.END_CLIENT_TICK.register(client -> {
			if (!isTracking) return;

			long now = System.currentTimeMillis();

			if (now - ConfigManager.config.lastBreakTime > PAUSE_THRESHOLD) {
				isTracking = false;
				ConfigManager.config.accumulatedActiveTime += (ConfigManager.config.lastBreakTime - ConfigManager.config.startBreakTime);
				ConfigManager.save();
			}
		});
	}
	public void resetFeverResult() {
		uncommon = 0;
		rare = 0;
		crazyRare = 0;
		prayToRNGESUS = 0;
		uncommonCrop = 0;
		rareCrop = 0;
		totalProfit = 0;
	}
	public void cropFeverResult() {
		Map<String, Integer> weightMap = Map.of(
				"UNCOMMON", uncommon,
				"RARE", rare,
				"CRAZY RARE", crazyRare,
				"PRAY TO RNGESUS", prayToRNGESUS
		);
		String[] grades = {"UNCOMMON", "RARE", "CRAZY RARE", "PRAY TO RNGESUS"};
		for(String grade: grades) {
			System.out.println(breakItem);
			JsonArray data = cropFeverData.get(breakItem).getAsJsonObject().get(grade).getAsJsonArray();
			if(data.get(1).getAsString().equals("UNCOMMON")) uncommonCrop += data.get(0).getAsInt() * weightMap.get(grade);
			else rareCrop += data.get(0).getAsInt() * weightMap.get(grade);
		}
		int profit = cropFeverData.get(breakItem).getAsJsonObject().get("Profit").getAsInt();
		totalProfit = (uncommonCrop*160 + rareCrop*25600)*profit;

	}
	public void sendFeverResult() {
		if (client.player == null) return;
		final String bi = this.breakItem;
		final int u = this.uncommon;
		final int r = this.rare;
		final int cr = this.crazyRare;
		final int ptr = this.prayToRNGESUS;
		final int uc = this.uncommonCrop;
		final int rc = this.rareCrop;
		final long profit = this.totalProfit;
		final long totalAmt = (long)uc * 160 + (long)rc * 25600;

		String pad = "            ";
		MutableComponent msg = Component.literal("==================================").withStyle(ChatFormatting.GRAY);

		msg.append(Component.literal("\n" + pad + "Crop Fever Result\n").withStyle(ChatFormatting.GOLD, ChatFormatting.BOLD));
		msg.append(Component.literal("\n"));

		// 드롭 통계 (한 줄씩 합치기)
		msg.append(buildLine("UNCOMMON DROP: ", String.valueOf(u), UNCOMMON));
		msg.append(buildLine("RARE DROP: ", String.valueOf(r), RARE));
		msg.append(buildLine("CRAZY RARE DROP: ", String.valueOf(cr), CRAZYRARE));
		msg.append(buildLine("PRAY TO RNGESUS: ", String.valueOf(ptr), PRAYTORNGESUS));
		msg.append(Component.literal("\n"));

		JsonObject itemObj = cropFeverData.get(bi).getAsJsonObject();
		String unName = itemObj.get("UN").getAsString();
		String rnName = itemObj.get("RN").getAsString();
		msg.append(Component.literal("Uncommon Crop : ").withStyle(s -> s.withColor(UNCOMMON))
				.append(Component.literal(uc + "x ").withStyle(ChatFormatting.WHITE)) // 숫자는 흰색
				.append(Component.literal(unName + "\n").withStyle(s -> s.withColor(UNCOMMON)))); // 아이템명은 색상

		msg.append(Component.literal("Rare Crop : ").withStyle(s -> s.withColor(RARE))
				.append(Component.literal(rc + "x ").withStyle(ChatFormatting.WHITE)) // 숫자는 흰색
				.append(Component.literal(rnName + "\n").withStyle(s -> s.withColor(RARE)))); // 아이템명은 색상
		msg.append(Component.literal("\n"));

		// 최종 요약
		msg.append(Component.literal("Crop Amount: ").append(Component.literal(formatAmount(totalAmt) + "\n")).withStyle(ChatFormatting.WHITE));

		msg.append(Component.literal("Total Profit: ").withStyle(s -> s.withColor(LEGENDARY))
				.append(Component.literal(String.format("%,d", profit)).withStyle(ChatFormatting.WHITE))
				.append(Component.literal(" coins\n").withStyle(s -> s.withColor(LEGENDARY))));

		msg.append(Component.literal("==================================").withStyle(ChatFormatting.GRAY));

		client.player.displayClientMessage(msg, false);
	}

	private MutableComponent buildLine(String label, String value, int color) {
		return Component.literal(label).withStyle(s -> s.withColor(color))
				.append(Component.literal(value + "\n").withStyle(ChatFormatting.WHITE));
	}
	public String formatAmount(long value) {
		if (value >= 1_000_000) {
			return String.format("%.1fM", value / 1_000_000.0);
		} else if (value >= 1_000) {
			return String.format("%.1fk", value / 1_000.0);
		}
		return String.valueOf(value);
	}
	public String getCrop(String message) {
		java.util.regex.Matcher m = java.util.regex.Pattern.compile("You dropped \\d+x (.+)").matcher(message);
		if (breakItem.equals("Unknown") || !cropFeverData.has(breakItem)) {
			System.out.println("DEBUG: 알 수 없는 작물입니다: " + breakItem);
		}
		if (!m.find()) return "Unknown";

		String item = m.group(1).toLowerCase();
		System.out.println(item) ;
		if (item.contains("sugar"))    return "Sugar Cane";
		if (item.contains("hay bale")) return "Wheat";
		if (item.contains("cookie"))   return "Cocoa Beans";
		if (item.contains("nether wart")) return "Nether Wart";
		if (item.contains("wild rose"))  return "Wild Rose";
		if (item.contains("mushroom"))   return item.contains("brown") ? "Brown Mushroom" : "Red Mushroom";

		String[] crops = {"wheat", "carrot", "potato", "pumpkin", "melon", "cactus", "moonflower", "sunflower"};

		for (String c : crops) {
			if (item.contains(c)) return c.substring(0, 1).toUpperCase() + c.substring(1);
		}

		return "Unknown";
	}
	public JsonObject loadCropFever() {
		try {
			InputStream is = getClass().getResourceAsStream("/cropfever.json");

			if (is == null) {
				System.out.println("파일을 찾을 수 없습니다! src/main/resources/cropfever.json 위치를 확인하세요.");
				return null;
			}

			try (InputStreamReader reader = new InputStreamReader(is, java.nio.charset.StandardCharsets.UTF_8)) {
				return com.google.gson.JsonParser.parseReader(reader).getAsJsonObject();
			}
		} catch (Exception e) {
			e.printStackTrace();
			return null;
		}
	}
	private void handleCropBreak() {
		long now = System.currentTimeMillis();
		ConfigManager.config.lastBreakTime = now;

		if (!isTracking) {
			isTracking = true;
			ConfigManager.config.startBreakTime = now;
			if (ConfigManager.config.lastFoundTime == 0) ConfigManager.config.lastFoundTime = now;
		}
	}
	private Component colorComponent(String string,int color) {
		return Component.literal(string).withColor(color);
	}
	private Component colorComponent(String string) {
		return colorComponent(string,0xFFFFFF);
	}
	private void showTimeDiffTitle() {
		client.gui.setTitle(colorComponent("RARE CROP! Helianthus",LEGENDARY));
		client.gui.setSubtitle(colorComponent(timeConverter(),UNCOMMON));
		client.gui.setTimes(10, 70, 20);
	}
	private String timeConverter(long diffMs) {
		long totalSeconds = diffMs / 1000;
		long hours = totalSeconds / 3600;
		long minutes = (totalSeconds % 3600) / 60;
		long seconds = (diffMs % 60000) / 1000;

		StringBuilder timeString = new StringBuilder();
		if (hours > 0) timeString.append(hours).append("h ");
		if (minutes > 0) timeString.append(minutes).append("m ");
		timeString.append(seconds).append("s");
        return timeString.toString();
	}
	private String timeConverter() {
		return timeConverter(getDiff());
	}

	private long getDiff() {
		if (ConfigManager.config.lastFoundTime == 0) return 0;

		if (isTracking) {
			return ConfigManager.config.accumulatedActiveTime + (System.currentTimeMillis() - ConfigManager.config.startBreakTime);
		} else {
			return ConfigManager.config.accumulatedActiveTime;
		}
	}
	private void playNote(LocalPlayer player, float pitch) {
		player.level().playLocalSound(
				player.getX(), player.getY(), player.getZ(),
				SoundEvents.NOTE_BLOCK_CHIME.value(),
				SoundSource.RECORDS,
				1.0F,
				pitch,
				false
		);
	}

	public void playGradeSound(String grade) {
		LocalPlayer player = Minecraft.getInstance().player;
		if (player == null) return;

		switch (grade) {
			case "UNCOMMON":
				playNote(player, 1.5F);
				break;

			case "RARE":
				playNote(player, 1.2F);
				new Thread(() -> {
					try { Thread.sleep(100); } catch (InterruptedException e) {}
					playNote(player, 1.5F);
				}).start();
				break;

			case "CRAZY RARE":
				float[] crazyPitches = {1.0F, 1.2F, 1.5F, 1.8F};
				playArpeggio(player, crazyPitches, 80);
				break;

			case "PRAY TO RNGESUS":
				float[] godPitches = {0.8F, 1.0F, 1.2F, 1.4F, 1.6F, 1.8F, 2.0F};
				playArpeggio(player, godPitches, 60);
				break;
		}
	}

	private void playArpeggio(LocalPlayer player, float[] pitches, int delay) {
		new Thread(() -> {
			for (float p : pitches) {
				playNote(player, p);
				try { Thread.sleep(delay); } catch (InterruptedException e) {}
			}
		}).start();
	}
}
