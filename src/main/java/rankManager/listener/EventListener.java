package rankManager.listener;

import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.Map;

import cn.nukkit.event.EventPriority;
import cn.nukkit.event.server.DataPacketReceiveEvent;
import cn.nukkit.level.Sound;
import cn.nukkit.level.particle.HugeExplodeParticle;
import cn.nukkit.network.protocol.SetLocalPlayerAsInitializedPacket;
import cn.nukkit.scheduler.TaskHandler;
import org.apache.commons.lang3.StringUtils;

import cn.nukkit.Player;
import cn.nukkit.command.Command;
import cn.nukkit.command.CommandSender;
import cn.nukkit.event.EventHandler;
import cn.nukkit.event.Listener;
import cn.nukkit.event.block.BlockBreakEvent;
import cn.nukkit.event.block.SignChangeEvent;
import cn.nukkit.event.player.PlayerChatEvent;
import cn.nukkit.event.player.PlayerFormRespondedEvent;
import cn.nukkit.event.player.PlayerInteractEvent;
import cn.nukkit.event.player.PlayerJoinEvent;
import cn.nukkit.event.player.PlayerKickEvent;
import cn.nukkit.event.player.PlayerQuitEvent;
import cn.nukkit.form.element.Element;
import cn.nukkit.form.element.ElementButton;
import cn.nukkit.form.element.ElementInput;
import cn.nukkit.form.element.ElementLabel;
import cn.nukkit.form.window.FormWindowCustom;
import cn.nukkit.form.window.FormWindowModal;
import cn.nukkit.form.window.FormWindowSimple;
import cn.nukkit.utils.TextFormat;
import me.onebone.economyapi.EconomyAPI;
import rankManager.RankManager;
import rankManager.listener.other.ListenerLoader;
import rankManager.rank.RankData;
import rankManager.rank.RankLoader;
import rankManager.rank.RankProvider;
import rankManager.task.ShowPrefixTitleTask;

public class EventListener implements Listener {
	public static final DecimalFormat df = new DecimalFormat("#,###");
	public static final int SELLING_LIST_FORM_ID = 28400;
	public static final int MAIN_FORM_ID = 28401;
	public static final int SELECTING_PLAYER_FORM_ID = 28402;
	public static final int PREFIX_LIST_FORM_ID = 28403;
	public static final int SELECT_PREFIX_FOR_DELETE_FORM_ID = 28404;
	public static final int ADD_PREFIX_FORM_ID = 28405;
	public static final int INPUT_PLAYER_FOR_DEL_PREFIX_FORM_ID = 28406;
	public static final int CHANGE_NICK_FORM_ID = 28407;
	public static final int CHANGE_PREFIX_FOR_ADMIN_FORM_ID = 28408;
	public final LinkedHashMap<String, String> uiPrefixSet = new LinkedHashMap<>();
	public final LinkedHashMap<String, String> uiSeeOtherPlayersPrefix = new LinkedHashMap<>();
	public final LinkedHashMap<String, String> uiDeletePrefix = new LinkedHashMap<>();
	public final LinkedHashMap<String, String> uiAdminDeletePrefix = new LinkedHashMap<>();
	public final LinkedHashMap<String, String> uiAdminDeleteSelPrefix = new LinkedHashMap<>();
	public final LinkedHashMap<String, String> uiAdminChangePrefix = new LinkedHashMap<>();
	public final LinkedHashMap<String, Integer> uiSaveCostForSelling = new LinkedHashMap<>();
	public final LinkedHashMap<String, String> selectedSeller = new LinkedHashMap<>();
	
	private static final SimpleDateFormat sdf = new SimpleDateFormat("yyyy년 MM월 dd일 HH시 mm분 ss초");
	private final RankManager plugin;
	private final RankLoader loader;
	private final RankProvider provider;
	private final ListenerLoader listenerLoader;

	public EventListener(RankManager plugin) {
		this.plugin = plugin;
		this.loader = plugin.getRankLoader();
		this.provider = plugin.getRankProvider();
		this.listenerLoader = plugin.getListenerLoader();

		plugin.getServer().getPluginManager().registerEvents(this, plugin);
	}

	public static boolean isNumeric(String s) {
		try {
			Double.parseDouble(s);
			return true;
		} catch (NumberFormatException e) {
			return false;
		}
	}

	public final String numberFormatting(int num) {
		return df.format(num);
	}

	@EventHandler
	public void onPlayerJoin(PlayerJoinEvent e) {
		RankData rankData = this.loader.getRank(e.getPlayer());
		if (this.provider.getDefaultPrefix() != null) {
			if (!rankData.isExistPrefix(this.provider.getDefaultPrefix())) {
				rankData.addPrefixes(new String[] { this.provider.getDefaultPrefix() }, 1);
			}
			if(rankData.getPrefix() == null) {
				rankData.setPrefix(this.provider.getDefaultPrefix());
			}
		}
		//this.provider.applyNameTag(e.getPlayer().getName());
		e.getPlayer().setNameTag(e.getPlayer().getName() + "님\n§l§c§o접속중...");
	}

	@EventHandler
	public void onDataPacket(DataPacketReceiveEvent ev){
		if(ev.getPacket() instanceof SetLocalPlayerAsInitializedPacket){
			this.provider.applyNameTag(ev.getPlayer().getName());
		}
	}

	@EventHandler
	public void onPlayerQuitEvent(PlayerQuitEvent e) {
		this.loader.unloadRank(e.getPlayer().getName());
	}

	@EventHandler
	public void onPlayerKickEvent(PlayerKickEvent e) {
		this.loader.unloadRank(e.getPlayer().getName());
	}

	@EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
	public void onPlayerChatEvent(PlayerChatEvent e) {
		e.setFormat(this.provider.applyChatFormat(e.getPlayer().getName(), "§7" + e.getMessage()));
	}

	@EventHandler
	public void onSignChangeEvent(SignChangeEvent e) {
		if (!e.getPlayer().hasPermission("rankmanager.rankshop.create"))
			return;
		String line = e.getLine(0);
		if ("칭호판매".equals(line)) {
			if (e.getLine(1) == null || e.getLine(2) == null || !isNumeric(e.getLine(2))) {
				this.plugin.message(e.getPlayer(), "도움말: §d1줄§f->칭호판매, §d2줄§f->판매원하는칭호명, §d3줄§f->가격 , §d4줄§f->가치값");
				return;
			}
			
			if (e.getLine(3) == null || !isNumeric(e.getLine(3))) {
				this.plugin.message(e.getPlayer(),
						"4번째 줄엔 §d가치값§f를 적어주세요.\n가치값는 칭호를 판매할 때 사용하여 얻기 어려울수록 §d높은§f 값을 가져야합니다.\n최대 §d500만§f 까지 허용됩니다.\n§c칭호상점의 경우 가치값과 판매가를 같게 설정하세요");
				return;
			}
			
			if (Integer.parseInt(e.getLine(3).trim()) < 1 || Integer.parseInt(e.getLine(3).trim()) > 5000000) {
				this.plugin.message(e.getPlayer(), "4번째 줄의 가치값이 1원보다 작거나 5백만원을 초과합니다.");
				return;
			}
			
			String requestedPrefix = e.getLine(1);
			String levelName = e.getBlock().getLevel().getName();
			int requestedPrice = Integer.parseInt(e.getLine(2).trim());
			double x = e.getBlock().getX();
			double y = e.getBlock().getY();
			double z = e.getBlock().getZ();
			this.provider.setRankShop(levelName, x, y, z, requestedPrefix, requestedPrice,
					Integer.parseInt(e.getLine(3).trim()));
			String formattedPrefix = this.provider.applyPrefixFormat(requestedPrefix);
			e.setLine(0, "§o§f[ §d터치§f로 칭호구매 ]");
			e.setLine(1, StringUtils.replace("%prefix%", "%prefix%", formattedPrefix, 1));
			e.setLine(2, StringUtils.replace("§d가격 §d: §d%price%§f원", "%price%",
					numberFormatting(requestedPrice), 1));
			e.setLine(3, "§f가치값 §d: " + e.getLine(3).trim());
		}
	}

	@EventHandler
	public void onBlockBreakEvent(BlockBreakEvent event) {
		String levelName = event.getBlock().getLevel().getName();
		double x = event.getBlock().getX();
		double y = event.getBlock().getY();
		double z = event.getBlock().getZ();
		Map<String, Object> rankShop = this.provider.getRankShop(levelName, x, y, z);

		if (rankShop != null) {
			if (!event.getPlayer().hasPermission("rankmanager.rankshop.delete")) {
				event.setCancelled();
				this.plugin.alert(event.getPlayer(), "§c칭호판매대를 삭제할 권한이 없습니다 !");
				return;
			}
			this.provider.deleteRankShop(levelName, x, y, z);
			this.plugin.message(event.getPlayer(), "칭호판매대가 §d삭제§f되었습니다 !");
		}
	}

	@EventHandler
	public void onPlayerInteractEvent(PlayerInteractEvent event) {
		if(event.getAction() != PlayerInteractEvent.Action.RIGHT_CLICK_BLOCK) return;
		EconomyAPI economyAPI = this.listenerLoader.getEconomyAPI();
		String levelName = event.getBlock().getLevel().getName();
		double x = event.getBlock().getX();
		double y = event.getBlock().getY();
		double z = event.getBlock().getZ();
		Map<String, Object> rankShop = this.provider.getRankShop(levelName, x, y, z);

		if (rankShop == null) {
			return;
		}
		
		if (economyAPI == null) {
			this.plugin.alert(event.getPlayer(), "이코노미 API가 없습니다, 구매 불가능 !");
			return;
		}
		
		event.setCancelled();
		
		if (!event.getPlayer().hasPermission("rankmanager.rankshop.use")) {
			this.plugin.alert(event.getPlayer(), "칭호를 구매할 수 있는 권한이 없습니다 !");
			return;
		}

		double myMoney = economyAPI.myMoney(event.getPlayer());
		
		if ((int) rankShop.get("price") > myMoney) {
			this.plugin.message(event.getPlayer(),"돈이 §d부족§f합니다 !");
			return;
		}

		RankData rankData = this.loader.getRank(event.getPlayer());
		String prefix = (String) rankShop.get("prefix");
		if (rankData.isExistPrefix(prefix)) {
			this.plugin.alert(event.getPlayer(), "이미구매한 칭호입니다. §c구매할 수 없습니다§f !");
			this.plugin.alert(event.getPlayer(), "( §d/칭호 변경 칭호번호§f 또는 §d칭호 UI§f에서 변경 가능합니다 ! )");
			return;
		}

		String[] temp = { prefix };
		economyAPI.reduceMoney(event.getPlayer(), (Integer) rankShop.get("price"));
		rankData.addPrefixes(temp, (Integer) rankShop.get("prePrice"));
		rankData.setPrefix(temp[0]);
		this.plugin.message(event.getPlayer(), "해당 칭호를 §d성공적으로§f 구매했습니다 !");
		this.plugin.message(event.getPlayer(), "§d/칭호 §f를 입력해보세요!");

		event.getPlayer().getLevel().addParticle(new HugeExplodeParticle(event.getPlayer().getLocation()));
		ShowPrefixTitleTask task = new ShowPrefixTitleTask(event.getPlayer(), prefix);
		TaskHandler handler = this.plugin.getServer().getScheduler().scheduleRepeatingTask(this.plugin, task, 10);
		task.setTaskHandler(handler);
	}

	public void showSimpleForm(Player player, String title, String content) {
		FormWindowSimple form = new FormWindowSimple(title, content);
		player.showFormWindow(form);
		return;
	}

	public void showSellingListForm(Player player) {
		LinkedHashMap<String, LinkedHashMap<String, Object>> list = this.provider.getSellingList();
		
		if (list.size() < 1) {
			this.showSimpleForm(player, "§l§8칭호 거래소",
					"§f§l현재 §d판매중인§f 칭호가 없습니다!\n§r§f칭호를 판매하려면 §d칭호 목록§f에서 원하는 칭호를 선택하고 §d판매하기 §f버튼을 터치하세요!");
			return;
		}
		
		ArrayList<ElementButton> buttons = new ArrayList<>();

		list.forEach((key, data) -> {
			String leftTime = "";
			int date = (int) data.get("date") - (int) (System.currentTimeMillis() / 1000);
			
			if ((date / 3600) > 0) {
				leftTime = "§d" + (int) Math.floor(date / 3600) + "§f시간 §d" + (int) Math.floor((date % 3600) / 60)
						+ "§f분";
			} else if ((date / 60) > 0) {
				leftTime = "§c" + (int) Math.floor((date % 3600) / 60) + "§f분";
			}
			
			buttons.add(new ElementButton("§8§l칭호 §f: §r" + data.get("prefix") + "\n§r§l§8판매가 : §d"
					+ numberFormatting((int) data.get("price")) + "\n§8판매자 §f: " + key + "\n남은 시간 : " + leftTime));
		});
		
		FormWindowSimple form = new FormWindowSimple("§l§8칭호 거래소",
				"§f§l총 §d" + list.size() + "§f개의 칭호가 등록되어 있습니다.", buttons);
		player.showFormWindow(form, SELLING_LIST_FORM_ID);
	}

	public void showMainForm(Player player) {
		ArrayList<ElementButton> buttons = new ArrayList<>();
		buttons.add(new ElementButton("§l§8칭호 목록"));
		buttons.add(new ElementButton("§l§8칭호 삭제"));
		buttons.add(new ElementButton("§l§8칭호 확인"));
		buttons.add(new ElementButton("§l§8칭호 거래소"));
		if (player.hasPermission("rankmanager.rank.control")) {
			buttons.add(new ElementButton("§l§8칭호 추가(관리자)"));
			buttons.add(new ElementButton("§l§8칭호 삭제(관리자)"));
			buttons.add(new ElementButton("§l§8칭호 변경(관리자)"));
			buttons.add(new ElementButton("§l§8닉네임 변경(관리자)"));
		}
		FormWindowSimple form = new FormWindowSimple("§8칭호", "§f§dTIP §f: /칭호 ? 명령어로 칭호 명령어를 사용할 수 있습니다.", buttons);
		player.showFormWindow(form, MAIN_FORM_ID);
	}

	public void showSelectingPlayerForm(Player player) {
		ElementInput input = new ElementInput("", "플레이어 이름 입력");
		ElementLabel label = new ElementLabel("§f특정 플레이어의 칭호목록을 확인할 수 있습니다.");
		FormWindowCustom form = new FormWindowCustom("§l§8칭호 확인");
		form.addElement(label);
		form.addElement(input);
		player.showFormWindow(form, SELECTING_PLAYER_FORM_ID);
	}

	public void showPrefixListUi(Player player) {
		ArrayList<ElementButton> buttons = new ArrayList<>();
		LinkedHashMap<String, LinkedHashMap<String, Integer>> target = this.loader.getRank(player).getPrefixList();

		Object[] index_key = target.keySet().toArray();
		for (Object prefix : index_key) {
			buttons.add(new ElementButton((String) prefix));
		}
		FormWindowSimple form = new FormWindowSimple("§8§l칭호 목록", "§l§f칭호 " + target.size() + "개", buttons);

		player.showFormWindow(form, PREFIX_LIST_FORM_ID);
	}

	@EventHandler
	public void onRespond(PlayerFormRespondedEvent ev) {
		Player player = ev.getPlayer();
		int formId = ev.getFormID();
		if (ev.getWindow() instanceof FormWindowSimple) {
			FormWindowSimple window = (FormWindowSimple) ev.getWindow();
			if (window.getResponse() == null) {
				return;
			}
			
			final ElementButton button = window.getResponse().getClickedButton();
			
			if (formId == MAIN_FORM_ID && button != null) {
				String clickedButton = window.getResponse().getClickedButton().getText();
				
				if (clickedButton.equals("§l§8칭호 목록")) {
					showPrefixListUi(player);
				}else if (clickedButton.equals("§l§8칭호 확인")) {
					showSelectingPlayerForm(player);
				} else if (clickedButton.equals("§l§8칭호 경매")) {

				} else if (clickedButton.equals("§l§8칭호 삭제")) {
					ArrayList<ElementButton> buttons = new ArrayList<>();
					LinkedHashMap<String, LinkedHashMap<String, Integer>> target = this.loader.getRank(player)
							.getPrefixList();

					Object[] index_key = target.keySet().toArray();
					for (Object prefix : index_key) {
						buttons.add(new ElementButton((String) prefix));
					}
					buttons.add(new ElementButton("§l§8돌아가기"));
					this.plugin.getLogger().info(player.getName() + "님이 칭호삭제 조회");
					FormWindowSimple form = new FormWindowSimple("§8§l칭호 삭제", "§l§c삭제§f할 칭호를 선택하세요.", buttons);
					player.showFormWindow(form, SELECT_PREFIX_FOR_DELETE_FORM_ID);
				} else if (clickedButton.equals("§l§8칭호 추가(관리자)")) {
					ArrayList<Element> elements = new ArrayList<>();
					elements.add(new ElementInput("§f§l추가할 대상의 닉네임"));
					elements.add(new ElementInput("§f§l추가할 칭호"));
					FormWindowCustom form = new FormWindowCustom("§8§l칭호 추가(관리자)", elements);
					player.showFormWindow(form, ADD_PREFIX_FORM_ID);
				} else if (clickedButton.equals("§l§8칭호 삭제(관리자)")) {
					ArrayList<Element> elements = new ArrayList<>();
					elements.add(new ElementInput("§f§l삭제할 대상의 닉네임"));
					FormWindowCustom form = new FormWindowCustom("§8§l칭호 삭제(관리자)", elements);
					player.showFormWindow(form, INPUT_PLAYER_FOR_DEL_PREFIX_FORM_ID);
				} else if (clickedButton.equals("§l§8닉네임 변경(관리자)")) {
					ArrayList<Element> elements = new ArrayList<>();
					elements.add(new ElementLabel(
							"§f§l1.닉네임이 너무 길진 않은지 확인하세요.\n2.닉네임이 적절한 내용인지 확인하세요.\n3.무단으로 유저의 닉네임을 변경하고있진 않은지 확인하세요.\n\n"));
					elements.add(new ElementInput("닉네임 입력칸(비워두면 본인)"));
					elements.add(new ElementInput("§l§f닉네임"));
					FormWindowCustom form = new FormWindowCustom("§8§l닉네임 변경", elements);
					player.showFormWindow(form, CHANGE_NICK_FORM_ID);
				} else if (clickedButton.equals("§l§8칭호 변경(관리자)")) {
					ArrayList<Element> elements = new ArrayList<>();
					elements.add(new ElementInput("§f§l칭호를 변경할 대상의 닉네임"));
					FormWindowCustom form = new FormWindowCustom("§8§l칭호 변경(관리자)", elements);
					player.showFormWindow(form, CHANGE_PREFIX_FOR_ADMIN_FORM_ID);
				} else if (clickedButton.equals("§l§8칭호 거래소")) {
					showSellingListForm(player);
					return;
				}
			} else if (formId == SELLING_LIST_FORM_ID) {
				if (button.getText() == null) {
					return;
				}
				
				if (button.getText().equals("")) {
					return;
				}
				
				String sellerName = button.getText().split("판매자 §f: ")[1].split("\n")[0];
				
				if (!this.provider.isSelling(sellerName)) {
					showSimpleForm(player, "§l§8칭호 거래소", "§f§l해당 칭호는 방금 판매가 끝난 칭호입니다 ㅜㅜ");
					return;
				}
				
				LinkedHashMap<String, Object> data = this.provider.getSellingInfo(sellerName);
				ArrayList<ElementButton> buttons = new ArrayList<>();
				
				if (sellerName.equals(player.getName())) {
					buttons.add(new ElementButton("§c§l판매 중단"));
					buttons.add(new ElementButton("§8§l돌아가기"));
				} else {
					buttons.add(new ElementButton("§8§l구매할래요"));
					buttons.add(new ElementButton("§8§l돌아가기"));

				}
				
				String lefttime;
				
				int date = (int) data.get("date") - (int) (System.currentTimeMillis() / 1000);
				if ((date / 3600) >= 1) {
					lefttime = new StringBuilder()
							.append("§d")
							.append((int) Math.floor(date / 3600))
							.append("§f시간 §d")
							.append((int) Math.floor((date % 3600) / 60))
							.append("§f분").toString();
				} else {
					lefttime = "§c" + (int) Math.floor(date / 60) + "§f분";
				}
				
				String content = new StringBuilder()
						.append("§d§l칭호 §f: §r")
						.append(data.get("prefix"))
						.append("\n§d§l판매가 §f: §d")
						.append(data.get("price"))
						.append("\n§d가치값 §f: §d")
						.append(data.get("cost"))
						.append("\n§d남은 시간 §f: §d")
						.append(lefttime).toString();
				
				FormWindowSimple form = new FormWindowSimple("§l§8칭호 거래소", content, buttons);
				this.selectedSeller.put(player.getName(), sellerName);
				player.showFormWindow(form, 7755);
				return;
			} else if (formId == 7755) {
				if (button.getText().equals(""))
					return;
				if (button.getText().equals("§8§l돌아가기")) {
					showSellingListForm(player);
					return;
				}
				if (button.getText().equals("§c§l판매 중단")) {
					this.provider.removeSeller(player.getName());
					showSimpleForm(player, "§l§8칭호 거래소", "§f§l성공적으로 판매가 중단되었습니다.");
					return;
				}
				if (!button.getText().equals("§8§l구매할래요"))
					return;
				String sellerName = this.selectedSeller.get(player.getName());
				if (sellerName == null) {
					showSimpleForm(player, "§l§8칭호 거래소", "§l§f서버 오류가 발생하였습니다.");
					return;
				}
				if (!this.provider.isSelling(sellerName)) {
					showSimpleForm(player, "§l§8칭호 거래소", "§f§l해당 칭호는 판매가 끝난 칭호입니다.");
					return;
				}
				if (sellerName.equals(player.getName())) {
					showSimpleForm(player, "§l§8칭호 거래소", "§f§l자신의 칭호는 구매할 수 없습니다.");
					return;
				}
				EconomyAPI economyAPI = this.listenerLoader.getEconomyAPI();
				LinkedHashMap<String, Object> data = this.provider.getSellingInfo(sellerName);
				if (economyAPI.myMoney(player) < (int) data.get("price")) {
					showSimpleForm(player, "§l§8칭호 거래소",
							"§8§l돈이 §c부족§f합니다.\n판매가 : " + data.get("cost") + "\n보유 금액 : " + economyAPI.myMoney(player));
					return;
				}
				RankData rankData = this.loader.getRankByName(sellerName);
				String prefix = (String) data.get("prefix");
				if (!rankData.isExistPrefix(prefix)) {
					showSimpleForm(player, "§l§8칭호 거래소", "§f판매자가 판매를 §d중단§f했습니다.");
					return;
				}
				if (rankData.getPrefixList().size() < 2) {
					showSimpleForm(player, "§l§8칭호 거래소", "§f죄송합니다.\n해당 칭호를 일시적으로 구매할 수 없습니다.");
					return;
				}
				rankData.deletePrefixes(new String[] { prefix });
				if (rankData.getPrefix() != null) {
					if (rankData.getPrefix().equals(prefix)) {
						double random = Math.random();
						int randomID = (int) (random * (rankData.getPrefixList().size() - 1));
						if (rankData.isExistPrefixToIndex(randomID)) {
							rankData.setPrefix(rankData.getPrefixByIndex(randomID));
							this.provider.applyNameTag(sellerName);
						}
					}
				}
				RankData playerRankData = this.loader.getRankByName(player.getName());
				playerRankData.addPrefixes(new String[] { (String) data.get("prefix") }, (int) data.get("cost"));
				economyAPI.reduceMoney(player, (int) data.get("price"));
				economyAPI.addMoney(sellerName, (int) data.get("price"));
				Player sellerPlayer = this.plugin.getServer().getPlayerExact(sellerName);
				this.plugin.getServer().broadcastMessage(new StringBuilder("§l§d")
						.append(player.getName())
						.append("§f님이 §d")
						.append(sellerName)
						.append("§f님의 칭호 §r")
						.append(data.get("prefix"))
						.append("§r§l§f를 구매하셨습니다!").toString());
				if (sellerPlayer != null) {
					sellerPlayer.sendTitle("§f§l칭호 판매 §d성공§f!", "§f앗싸! §e" + data.get("price") + "§f원 얻었다!", 20, 40,
							20);
				} else {
					plugin.addMail(sellerName, new StringBuilder()
							.append("§l§f[ 칭호 ] §d")
							.append(player.getName())
							.append("§f님이 §r")
							.append(data.get("prefix"))
							.append("§r§l를 §d ")
							.append(data.get("price"))
							.append("§f원에 구매하셨습니다.").toString());
				}
				this.plugin.showBossbar(new StringBuilder()
						.append("§d§l")
						.append(player.getName())
						.append("§f님이 §d")
						.append(sellerName)
						.append("§f님의 칭호 §r")
						.append(data.get("prefix"))
						.append("§r§l를 구매하였습니다.").toString());
				this.provider.removeSeller(sellerName);
				showSimpleForm(player, "§l§8칭호 거래소", "§f§l성공적으로 §r" + data.get("prefix") + " §r§f§l를 구매하셨습니다.");
			} else if (formId == PREFIX_LIST_FORM_ID) {
				String selectedPrefix = window.getResponse().getClickedButton().getText();
				if (selectedPrefix == null) {
					return;
				}
				if (selectedPrefix.trim().equals("")) {
					return;
				}
				String perchasedDate = "";
				int costs = 1;
				LinkedHashMap<String, LinkedHashMap<String, Integer>> target = this.loader.getRank(player)
						.getPrefixList();
				Object[] index_key = target.keySet().toArray();
				for (Object prefix : index_key) {
					if (!prefix.equals(selectedPrefix))
						continue;
					costs = target.get(prefix).get("costs");
					perchasedDate = sdf
							.format(target.get(prefix).get("date") * 1000L);
					break;
				}
				ArrayList<ElementButton> buttons = new ArrayList<>();
				buttons.add(new ElementButton("§8§l칭호로 설정하기"));
				buttons.add(new ElementButton("§8§l판매하기"));
				buttons.add(new ElementButton("§8§l돌아가기"));
				FormWindowSimple prefixInfoWindow = new FormWindowSimple("§8§l칭호 정보", new StringBuilder()
						.append(RankManager.PREFIX_NORMAL).append("칭호 : §r").append(selectedPrefix).append("\n")
						.append(RankManager.PREFIX_NORMAL).append("얻은 날짜 :").append(perchasedDate).append("\n")
						.append(RankManager.PREFIX_NORMAL).append("가치 : ").append(numberFormatting(costs))
						.append("원\n\n").toString(),
						buttons);
				this.uiPrefixSet.put(player.getName(), selectedPrefix);
				this.plugin.getLogger().info(player.getName() + "님이 칭호 : " + selectedPrefix + "의 정보를 조회하셨습니다.");
				player.showFormWindow(prefixInfoWindow, 1890);
			} else if (formId == 1890) {
				String playerName = player.getName();
				String buttonText = window.getResponse().getClickedButton().getText();
				if (buttonText == null)
					return;
				if (buttonText.trim().equals(""))
					return;
				if (buttonText.equals("§8§l칭호로 설정하기")) {
					String selectedPrefix = this.uiPrefixSet.get(playerName);
					if (selectedPrefix == null) {
						this.plugin.message(player, "오류가 발생하였습니다 : §d17");
						this.plugin.getLogger().error(playerName + "님의 칭호 설정 요청이 오류로 인해 거부되었습니다.");
						return;
					}
					RankData rankData = this.loader.getRank(player);
					rankData.setPrefix(selectedPrefix);
					this.provider.applyNameTag(playerName);
					player.showFormWindow(new FormWindowSimple("§8§l칭호 설정",
							"§l§d성공적으로 §f칭호가 §r" + selectedPrefix + "§r§f§l으로 설정되었습니다!"));
					this.plugin.getLogger().info(playerName + "님의 칭호가 " + selectedPrefix + "으로 설정되었습니다.");
					this.uiPrefixSet.remove(playerName);
				} else if (buttonText.equals("§8§l판매하기")) {
					ArrayList<Element> elements = new ArrayList<>();
					LinkedHashMap<String, LinkedHashMap<String, Integer>> rd = this.provider.getRank(player)
							.getPrefixList();
					int cost = rd.get(this.uiPrefixSet.get(playerName))
							.get("costs");
					this.uiSaveCostForSelling.put(playerName, cost);
					elements.add(new ElementLabel(new StringBuilder()
							.append("§f§l정말 ")
							.append(this.uiPrefixSet.get(playerName))
							.append(" §r§l 를 판매하실건가요?\n해당 칭호의 가치는 §d")
							.append(numberFormatting(cost))
							.append("§f원 입니다.\n해당 칭호는 §d")
							.append((cost / 2) > 0 ? (cost / 2) : 1)
							.append(" ~ 500백만원§f의 가격으로 판매할 수 있습니다.").toString()));
					elements.add(new ElementInput("", "판매가"));
					FormWindowCustom cutomForm = new FormWindowCustom("§8§l칭호 판매", elements);
					player.showFormWindow(cutomForm, 4900);
				} else {
					showPrefixListUi(player);
				}
			} else if (formId == 15099) { // 칭호 확인 플레이어 이름 없음
				ElementButton clickedButton = window.getResponse().getClickedButton();
				if (clickedButton != null) {
					if (clickedButton.getText().equals("§8§l돌아가기")) {
						showSelectingPlayerForm(player);
					} else if (clickedButton.getText().equals("§8§l메인으로")) {
						showMainForm(player);
					}
				}
			} else if (formId == 1530) { // 다른 사람 칭호 목록
				String targetPlayer = this.uiSeeOtherPlayersPrefix.get(player.getName());
				if (targetPlayer == null) {
					this.plugin.message(player, "오류가 발생하였습니다 : §d16");
					this.plugin.getLogger().error(player.getName() + "님의 칭호 확인이 오류로 인해 거부되었습니다.");
					return;
				}

				String buttonText = window.getResponse().getClickedButton().getText();
				if (buttonText == null)
					return;
				if (buttonText.trim().equals(""))
					return;
				String perchaseDate = "";
				int costs = 1;
				LinkedHashMap<String, LinkedHashMap<String, Integer>> target = this.loader.getRankByName(targetPlayer)
						.getPrefixList();
				Object[] index_key = target.keySet().toArray();
				for (Object prefix : index_key) {
					if (!prefix.equals(buttonText))
						continue;
					costs = target.get(prefix).get("costs");
					perchaseDate = sdf
							.format(target.get(prefix).get("date") * 1000L);
					break;
				}
				ArrayList<ElementButton> buttons = new ArrayList<>();
				buttons.add(new ElementButton("§8§l메인으로"));
				FormWindowSimple prefixInfoWindow = new FormWindowSimple(new StringBuilder()
						.append("§8§l§o")
						.append(targetPlayer)
						.append("§r§o§8의 칭호 정보").toString(),
						new StringBuilder()
								.append("§d§l칭호§f : §r")
								.append(buttonText)
								.append("\n§r§l§d얻은 날짜§f : §e")
								.append(perchaseDate)
								.append("\n§d가치 §f: §e")
								.append(numberFormatting(costs))
								.append("원\n\n").toString(),
						buttons);
				player.showFormWindow(prefixInfoWindow, 15922);
			} else if (formId == 15922) {
				if (window.getResponse().getClickedButton() != null) {
					showMainForm(player);
				}
			} else if (formId == SELECT_PREFIX_FOR_DELETE_FORM_ID) {
				if (window.getResponse().getClickedButton() == null)
					return;
				String clickedButtonText = window.getResponse().getClickedButton().getText();
				if (clickedButtonText == null)
					return;
				if (clickedButtonText.equals("§l§8돌아가기")) {
					showMainForm(player);
					return;
				}
				this.uiDeletePrefix.put(player.getName(), clickedButtonText);
				FormWindowModal modalForm = new FormWindowModal("§8§l칭호 삭제",
						new StringBuilder()
								.append("§l§f정말 ")
								.append(clickedButtonText)
								.append("§r§l§f칭호를 §c삭제§f하실건가요?\n§c이 작업은 되돌릴 수 없습니다!").toString(), "네", "아니오");
				player.showFormWindow(modalForm, 1922);
			} else if (formId == 1101) {
				String tartgetNick = this.uiAdminDeletePrefix.get(player.getName());
				if (tartgetNick == null)
					return;
				String clickedButtonString = button.getText();
				if (clickedButtonString == null)
					return;
				if (clickedButtonString.trim().equals(""))
					return;
				if (clickedButtonString.equals("§l§c취소하기")) {
					showMainForm(player);
					return;
				}
				FormWindowModal aModal = new FormWindowModal("§8§l칭호 삭제",
						new StringBuilder()
								.append("§l§f정말 §d")
								.append(tartgetNick)
								.append("§f님의 ")
								.append(clickedButtonString)
								.append("§r§f§l을 §c삭제§f하시겠어요?\n§c복구할 수 없어요! 물론 다시 추가하면 되겠지만 date값과 가치값은 되돌릴 수 없습니다!").toString(),
						"네", "아니오");
				this.uiAdminDeleteSelPrefix.put(player.getName(), clickedButtonString);
				player.showFormWindow(aModal, 1937);
				return;
			} else if (formId == 1325) {
				String targetName = this.uiAdminChangePrefix.get(player.getName());
				if (targetName == null)
					return;
				String targetPrefix = button.getText();
				if (targetPrefix.equals("§c§l취소하기")) {
					showMainForm(player);
					return;
				}
				RankData rankData = this.loader.getRankByName(targetName);
				if (!rankData.isExistPrefix(targetPrefix)) {
					showSimpleForm(player, "§8§l칭호 추가 (관리자)", "§f§l오류가 발생했어요.\n존재하지 않는 칭호입니다.");
					return;
				}
				rankData.setPrefix(targetPrefix);
				this.provider.applyNameTag(targetName);
				showSimpleForm(player, "§8§l칭호 추가 (관리자)",
						"§f§l성공적으로 §d" + targetName + "§f의 칭호를" + targetPrefix + "§r§f§l으로 설정했어요.");
				return;
			}

		} else if (ev.getWindow() instanceof FormWindowCustom) {
			FormWindowCustom window = (FormWindowCustom) ev.getWindow();
			if (window.getResponse() == null)
				return;
			if (formId == SELECTING_PLAYER_FORM_ID) {
				if (window.getResponse() == null)
					return;
				String targetPlayerName = window.getResponse().getInputResponse(1);
				if (targetPlayerName == null) {
					ArrayList<ElementButton> buttons = new ArrayList<>();
					buttons.add(new ElementButton("§8§l돌아가기"));
					buttons.add(new ElementButton("§8§l메인으로"));
					FormWindowSimple plzEnterName = new FormWindowSimple("§8§l칭호 확인", "§f§l플레이어 이름을 입력해주세요.", buttons);
					player.showFormWindow(plzEnterName, 15099);
				} else if (targetPlayerName.trim().equals("")) {
					ArrayList<ElementButton> buttons = new ArrayList<>();
					buttons.add(new ElementButton("§8§l돌아가기"));
					buttons.add(new ElementButton("§8§l메인으로"));
					FormWindowSimple plzEnterName = new FormWindowSimple("§8§l칭호 확인", "§f§l플레이어 이름을 입력해주세요.", buttons);
					player.showFormWindow(plzEnterName, 15099);
				} else {
					targetPlayerName = targetPlayerName.trim();
					Player player2 = this.plugin.getServer().getPlayer(targetPlayerName);
					if (player2 == null) {
						RankData rankData = this.loader.getRankByName(targetPlayerName);
						if (rankData.getPrefixList().size() > 0) {
							ArrayList<ElementButton> buttons = new ArrayList<>();
							rankData.getPrefixList().forEach((prefix, bool) -> buttons.add(new ElementButton((String) prefix)));
							this.plugin.getLogger().info(player.getName() + ",0,칭호확인," + targetPlayerName);
							this.uiSeeOtherPlayersPrefix.put(player.getName(), targetPlayerName);
							FormWindowSimple newWindow = new FormWindowSimple(new StringBuilder()
									.append("§8§l").append(targetPlayerName)
									.append("의 칭호 목록").toString(),
									"§l§f칭호 " + rankData.getPrefixList().size() + "개", buttons);
							player.showFormWindow(newWindow, 1530);
						} else {
							ArrayList<ElementButton> buttons = new ArrayList<>();
							buttons.add(new ElementButton("§8§l돌아가기"));
							buttons.add(new ElementButton("§8§l메인으로"));
							FormWindowSimple form = new FormWindowSimple("§8§l칭호 확인", "§f§l해당 플레이어를 찾을 수 없습니다.",
									buttons);
							player.showFormWindow(form, 15099);
						}
						return;
					}
					ArrayList<ElementButton> buttons = new ArrayList<>();
					LinkedHashMap<String, LinkedHashMap<String, Integer>> target = this.loader.getRank(player2)
							.getPrefixList();

					Object[] index_key = target.keySet().toArray();
					for (Object prefix : index_key) {
						buttons.add(new ElementButton((String) prefix));
					}
					this.plugin.getLogger().info(player.getName() + ",1,칭호확인," + player2.getName());
					this.uiSeeOtherPlayersPrefix.put(player.getName(), player2.getName());
					FormWindowSimple newWindow = new FormWindowSimple("§l§8" + player2.getName() + "의 칭호 목록",
							"§l§f칭호 " + target.size() + "개", buttons);
					player.showFormWindow(newWindow, 1530);

				}
			} else if (formId == CHANGE_NICK_FORM_ID) { // 닉네임
				if (!player.isOp())
					return;
				String targetnick = (String) window.getResponse().getResponse(1);
				String nick = (String) window.getResponse().getResponse(2);
				this.plugin.getLogger().info(player.getName() + "의 닉네임 변경 요청 : " + targetnick + " , " + nick);
				if (nick == null)
					return;
				if (nick.trim().equals("")) {
					FormWindowSimple simpeForm = new FormWindowSimple("§8§l닉네임 변경(관리자)", "변경할 닉네임칸이 비어있어요");
					player.showFormWindow(simpeForm);
					return;
				}
				if (targetnick == null) {
					targetnick = player.getName();
				} else if (targetnick.trim().equals("")) {
					targetnick = player.getName();
				}

				this.plugin.korea(targetnick, nick);
				FormWindowSimple simpleForm = new FormWindowSimple("§8§l닉네임 변경", new StringBuilder()
						.append("§f§d")
						.append(targetnick)
						.append("§f님의 닉네임이 ")
						.append(nick)
						.append("으로 변경되었어요. \n 참고로 유효하지 않은 닉네임인지 확인해 주셔야 해요.\n유효하지 않은 플레이어의 이름이라도 변경되거든요!").toString());
				player.showFormWindow(simpleForm);
				return;
			} else if (formId == INPUT_PLAYER_FOR_DEL_PREFIX_FORM_ID) { 
				if (!player.isOp()) {
					return;
				}
				
				String targetPlayerName = window.getResponse().getInputResponse(0);
				this.plugin.getLogger().info(player.getName() + "의 관리자 칭호 삭제 요청, target:" + targetPlayerName);
				
				if (targetPlayerName == null) {
					return;
				}
				
				if (targetPlayerName.trim().equals("")) {
					FormWindowSimple simpeForm = new FormWindowSimple("§8§l칭호 삭제(관리자)", "플레이어 이름칸이 비어있어요");
					player.showFormWindow(simpeForm);
					return;
				}
				
				ArrayList<ElementButton> buttons = new ArrayList<>();
				LinkedHashMap<String, LinkedHashMap<String, Integer>> target = this.loader.getRankByName(targetPlayerName)
						.getPrefixList();
				
				Object[] index_key = target.keySet().toArray();
				if (index_key.length < 1) {
					FormWindowSimple simpeForm = new FormWindowSimple("§8§l칭호 삭제(관리자)",
							"§f§l해당 유저는 칭호를 가지고 있지 않아요.\n플레이어 이름을 제대로 입력했는지 확인해주세요.\n입력된 플레이어 : " + targetPlayerName);
					player.showFormWindow(simpeForm);
					return;
				}
				
				for (Object prefix : index_key) {
					buttons.add(new ElementButton((String) prefix));
				}
				buttons.add(new ElementButton("§l§c취소하기"));
				
				FormWindowSimple form = new FormWindowSimple("§8§l칭호 삭제",
						"§l§c삭제§f할 칭호를 선택하세요.\n대상 플레이어 : " + targetPlayerName, buttons);
				this.plugin.getLogger().info(player.getName() + "님이 " + targetPlayerName + "의 칭호삭제 조회");
				this.uiAdminDeletePrefix.put(player.getName(), targetPlayerName);
				player.showFormWindow(form, 1101);
			} else if (formId == ADD_PREFIX_FORM_ID) {
				if (!player.isOp())
					return;
				String targetPlayerName = window.getResponse().getInputResponse(0);
				String targetPrefix = window.getResponse().getInputResponse(1);
				if (targetPlayerName == null || targetPrefix == null)
					return;
				if (targetPlayerName.trim().equals("") || targetPrefix.trim().equals("")) {
					showSimpleForm(player, "§8§l칭호 추가 (관리자)", "§f§l닉네임 또는 칭호칸이 비어있습니다.");
					return;
				}
				RankData rankData = this.loader.getRankByName(targetPlayerName);
				if (rankData.isExistPrefix(targetPrefix)) {
					showSimpleForm(player, "§8§l칭호 추가 (관리자)", "§f§l이미 존재하는 칭호입니다.");
					return;
				}
				rankData.addPrefixes(new String[] { targetPrefix.trim() }, 1);
				showSimpleForm(player, "§8§l칭호 추가 (관리자)",
						"§d§l성공§f적으로 §d " + targetPlayerName + "§f님에게 " + targetPrefix + "§r§f를 추가하였습니다");

				this.plugin.getLogger().info(player.getName() + "에 의해 " + targetPlayerName + "님에게" + targetPrefix + "가 추가됨");

				return;
			} else if (formId == CHANGE_PREFIX_FOR_ADMIN_FORM_ID) {
				if (!player.isOp()) {
					return;
				}
				
				String targetPlayerName = window.getResponse().getInputResponse(0);
				if (targetPlayerName == null) {
					return;
				}
				
				if (targetPlayerName.trim().equals("")) {
					showSimpleForm(player, "§8§l칭호 추가 (관리자)", "§f§l플레이어 닉네임 칸이 비어있습니다.");
					return;
				}
				
				RankData rankData = this.loader.getRankByName(targetPlayerName);
				if (rankData.getPrefixList().size() > 0) {
					ArrayList<ElementButton> buttons = new ArrayList<>();
					rankData.getPrefixList().forEach((prefix, bool) -> buttons.add(new ElementButton((String) prefix)));
					buttons.add(new ElementButton("§c§l취소하기"));
					
					this.plugin.getLogger().info(player.getName() + ",0,칭호확인," + targetPlayerName);
					this.uiAdminChangePrefix.put(player.getName(), targetPlayerName);
					
					FormWindowSimple form = new FormWindowSimple("§8§l" + targetPlayerName + "의 칭호 목록",
							"§l§f칭호 " + rankData.getPrefixList().size() + "개", buttons);
					player.showFormWindow(form, 1325);

				} else {
					showSimpleForm(player, "§8§l칭호 추가 (관리자)", "§f§l" + targetPlayerName + "는 존재하지 않는 플레이어입니다.");
					return;
				}

			} else if (formId == 4900) {
				String respond = window.getResponse().getInputResponse(1);
				if (respond.trim().equals("")) {
					showSimpleForm(player, "§8§l칭호 판매", "§f§l판매가가 비어있어 칭호를 판매할 수 없습니다 !");
					return;
				}
				this.plugin.getLogger().info(player.getName() + "님이" + respond + "원으로 판매 요청");
				
				if (!isNumeric(respond)) {
					showSimpleForm(player, "§8§l칭호 판매", "§f§l판매가는 무조건 §d숫자§f로 입력해야 합니다 !\n §c판매 불가능 !");
					return;
				}
				int SellPrice = (int) Math.floor(Double.parseDouble(respond));
				int cost = this.uiSaveCostForSelling.get(player.getName());
				if (SellPrice > 0 && SellPrice > (cost / 2) && SellPrice < 5000000) {
					if (this.provider.isSelling(player.getName())) {
						showSimpleForm(player, "§8§l칭호 판매",
								"§f§l이미 칭호를 판매하고 계십니다.\n칭호는 한번에 최대 §d1개§f까지 판매 가능하며, 추가로 판매하려면 §d24시간§f을 기다리거나 기존 칭호의 판매를 중단해주세요.");
						return;
					}
					
					this.provider.addSeller(player.getName(), cost, SellPrice, this.uiPrefixSet.get(player.getName()));
					showSimpleForm(player, "§8§l칭호 판매",
							"§l§a성공§f적으로 §d판매등록§f을 완료하였습니다.\n§d등록된 칭호 §f : " + this.uiPrefixSet.get(player.getName()));
					this.plugin.showBossbar(new StringBuilder()
							.append("§l§d")
							.append(player.getName())
							.append("§f님이 칭호 ")
							.append(this.uiPrefixSet.get(player.getName()))
							.append("§r§l§f를 §d")
							.append(SellPrice)
							.append("§f원에 판매중입니다.").toString());
					return;
				}
				showSimpleForm(player, "§8§l칭호 판매", "§l§d판매가§f가 §d" + ((cost / 2) > 0 ? (cost / 2) : 1)
						+ "§f보다 작거나 §d5000000§f보다 큽니다.\n§d판매 불가능 !");
				return;
			}
		} else if (ev.getWindow() instanceof FormWindowModal) {
			FormWindowModal window = (FormWindowModal) ev.getWindow();
			if (window.getResponse() == null)
				return;
			int clickedButton = window.getResponse().getClickedButtonId();
			if (formId == 1922) {
				String selectedPrefix = this.uiDeletePrefix.get(player.getName());
				if (selectedPrefix == null) {
					this.plugin.message(player, "§l§f오류가 발생하였습니다. 201");
					this.plugin.getLogger().error(player.getName() + "님의 칭호 삭제 요청 오류로 인해 취소됨");
					return;
				}
				if (clickedButton == 1) {
					this.plugin.getLogger().info(player.getName() + "님의 칭호 삭제 요청 취소됨");
					showMainForm(player);
				} else if (clickedButton == 0) {
					RankData rankData = this.provider.getRankByName(player.getName());
					if (!rankData.isExistPrefix(selectedPrefix)) {
						this.plugin.message(player, "§l§f오류가 발생하였습니다. 존재하지 않는 칭호입니다!");
						this.plugin.getLogger().error(player.getName() + "님의 유효하지 않는 칭호 삭제 요청 : " + selectedPrefix);
						return;
					}
					rankData.deletePrefixes(new String[] { selectedPrefix });
					if (rankData.getPrefix().equals(selectedPrefix)) {
						double random = Math.random();
						int randomID = (int) (random * (rankData.getPrefixList().size() - 1));
						if (rankData.isExistPrefixToIndex(randomID)) {
							rankData.setPrefix(rankData.getPrefixByIndex(randomID));
							this.provider.applyNameTag(player.getName());
						}
					}
					FormWindowSimple form = new FormWindowSimple("§8§l칭호 삭제",
							"§l§f성공적으로 " + selectedPrefix + "§r칭호가 §d삭제§f되었습니다.");
					this.uiDeletePrefix.remove(player.getName());
					player.showFormWindow(form);
					this.plugin.getLogger().info(player.getName() + "의 " + selectedPrefix + "§f삭제요청 성공");
				}
			} else if (formId == 1937) {
				if (clickedButton == 1) {
					showMainForm(player);
					return;
				}
				String tartget = this.uiAdminDeletePrefix.get(player.getName());
				String tartgetPrefix = this.uiAdminDeleteSelPrefix.get(player.getName());
				if (tartget == null)
					return;
				if (tartgetPrefix == null)
					return;
				RankData rankData = this.provider.getRankByName(tartget);
				if (!rankData.isExistPrefix(tartgetPrefix)) {
					this.plugin.message(player, "§l§f오류가 발생하였습니다. 존재하지 않는 칭호입니다!");
					this.plugin.getLogger().error(player.getName() + "님의 유효하지 않는 칭호 삭제 요청 (관리자) : " + tartgetPrefix);
					return;
				}
				rankData.deletePrefixes(new String[] { tartgetPrefix });
				if (rankData.getPrefix().equals(tartgetPrefix)) {
					if (rankData.isExistPrefixToIndex(0)) {
						rankData.setPrefix(rankData.getPrefixByIndex(0));
						this.provider.applyNameTag(tartget);
					}
				}
				FormWindowSimple aFormWindowSimple = new FormWindowSimple("§8§l칭호 삭제",
						new StringBuilder()
								.append("§l§f성공적으로 §d")
								.append(tartget)
								.append(" §f님의")
								.append(tartgetPrefix)
								.append("§r칭호가 §d삭제§f되었습니다.").toString());
				this.uiDeletePrefix.remove(player.getName());
				this.uiAdminDeletePrefix.remove(player.getName());
				this.uiAdminDeleteSelPrefix.remove(player.getName());
				player.showFormWindow(aFormWindowSimple);
				this.plugin.getLogger().info(player.getName() + "의 " + tartgetPrefix + "§f삭제요청 성공");

			}
		}
	}

	public void sendHelpMessages(CommandSender player) {
		this.plugin.message(player, "§d칭호 목록§f - 본인이 보유하고 있는 칭호목록을 표시합니다.");
		this.plugin.message(player, "§d칭호 변경 <칭호번호>§f - 해당칭호로 칭호를 변경합니다.");
		this.plugin.message(player, "§d칭호 삭제 <칭호번호>§f - 해당 칭호를 삭제합니다.");
		this.plugin.message(player, "§d칭호 확인 <유저명> §f- 해당유저의 칭호를 확인합니다.");
		this.plugin.message(player, "§d칭호 거래소 §f- 칭호 거래소 UI를 띄웁니다.");
		if (player.hasPermission("rankmanager.rank.control")) {
			this.plugin.message(player, "§d칭호 추가 <유저명> <칭호명> §f- 해당유저에게 해당칭호를 줍니다.");
			this.plugin.message(player, "§d칭호 삭제 <유저명> <칭호명> §f- 해당유저에게서 해당 칭호를 삭제합니다.");
			this.plugin.message(player, "§d칭호 닉네임 [닉네임] [이름] §f - 해당유저의 닉네임을 변경합니다.");
		}
	}

	public boolean onCommand(CommandSender player, Command command, String label, String[] args) {
		if (!(player instanceof Player)) {
			player.sendMessage("인게임에서 입력해주세요.");
		}
		if (!player.hasPermission("rankmanager.rank.manage"))
			return true;
		if (!command.getName().toLowerCase().equals("칭호"))
			return true;

		if (args.length == 0) {
			if (!(player instanceof Player)) {
				sendHelpMessages(player);
				return true;
			}
			showMainForm((Player) player);
			return true;
		}

		String string = args[0];
		if ("?".equals(string)) {
			sendHelpMessages(player);
			return true;
		}else if(string.equals("거래소")){
			this.showSellingListForm((Player) player);
			return true;
		}else if ("닉네임".equals(string)) {
			if (!player.hasPermission("rankmanager.rank.control"))
				return true;
			if (args.length < 3) {
				this.plugin.message(player, "§d칭호 닉네임 <닉네임> <이름> §f - 해당유저의 닉네임을 변경합니다.");
				return true;
			}
			this.plugin.korea(args[1], args[2]);
			this.plugin.message(player, args[1] + " 님의 닉네임이 " + args[2] + "로 설정 되었습니다.");
		} else if (string.equals("목록")) {
			int index;
			if (args.length > 1) {
				index = isNumeric(args[1]) ? Integer.parseInt(args[1]) : 1;
			} else {
				index = 1;
			}
			this.getPrefixList((Player) player, index);
		} else if (string.equals("변경")) {
			if (!(args.length > 1)) {
				this.plugin.message( player, "§d칭호 변경 <칭호번호>§f - 해당칭호로 칭호를 변경합니다.");
				return true;
			}
			if (!isNumeric(args[1])) {
				this.plugin.message(player, "§d칭호 변경 <칭호번호>§f - 해당칭호로 칭호를 변경합니다.");
				return true;
			}
			RankData rankData = this.loader.getRank((Player) player);
			if (!rankData.isExistPrefixToIndex(Integer.parseInt(args[1]))) {
				this.plugin.alert(player, "해당 칭호를 소유하고 있지 §c않습니다 §f!");
				return true;
			}
			rankData.setPrefix(rankData.getPrefixByIndex(Integer.parseInt(args[1])));
			this.provider.applyNameTag(player.getName());
			this.plugin.message(player, "칭호를 §d정상§f적으로 §d변경§f했습니다 !");
		} else if (string.equals("추가")) {
			if (!player.hasPermission("rankmanager.rank.control"))
				return true;
			if (!(args.length > 1)) {
				this.plugin.message(player, "§d칭호 추가 <유저명> <칭호명> §f- 해당유저에게 해당칭호를 줍니다.");
				return true;
			}
			if (!(args.length > 2)) {
				RankData rankData = this.loader.getRank((Player) player);
				if (rankData.isExistPrefix(args[1])) {
					this.plugin.alert(player, "해당 칭호를 §d이미§f 소유하고 있습니다 !");
					return true;
				}
				rankData.addPrefixes(new String[] { args[1] }, 1);
				this.plugin.message(player, "해당 칭호를 §d정상§f적으로 §d추가§f했습니다 !");
			} else {
				RankData rankData = this.loader.getRankByName(args[1]);
				if (rankData.isExistPrefix(args[2])) {
					this.plugin.alert(player, "해당 칭호를 §d이미§f 소유하고 있습니다 !");
					return true;
				}
				rankData.addPrefixes(new String[] { args[2] }, 1);
				this.plugin.message(player, "해당 칭호를 §d정상§f적으로 §d추가§f했습니다 !");
			}
		} else if (string.equals("삭제")) {
			if (!(args.length > 1)) {
				this.plugin.message(player, "§d칭호 삭제 <칭호번호>§f - 해당 칭호를 삭제합니다.");
				return true;
			}
			if (!(args.length > 2)) {
				if (!isNumeric(args[1])) {
					this.plugin.message(player, "§d칭호 삭제 <칭호번호>§f - 해당 칭호를 삭제합니다.");
					return true;
				}
				RankData rankData = this.loader.getRank((Player) player);
				if (!rankData.isExistPrefixToIndex(Integer.parseInt(args[1]))) {
					this.plugin.alert(player, "해당 칭호를 소유하고 있지 §c않습니다 §f!");
					return true;
				}
				String selectedPrefix = rankData.getPrefixByIndex(Integer.parseInt(args[1]));
				rankData.deletePrefixes(new String[] { rankData.getPrefixByIndex(Integer.parseInt(args[1])) });
				if (rankData.getPrefix().equals(selectedPrefix)) {
					double random = Math.random();
					int randomID = (int) (random * (rankData.getPrefixList().size() - 1));
					if (rankData.isExistPrefixToIndex(randomID)) {
						rankData.setPrefix(rankData.getPrefixByIndex(randomID));
						this.provider.applyNameTag(player.getName());
					}
				}
				this.plugin.message(player, "해당 칭호를 §d정상§f적으로 §c삭제§f했습니다 !");
			} else {
				if (!player.hasPermission("rankmanager.rank.control"))
					return true;
				if (!isNumeric(args[2])) {
					this.plugin.message(player, "§d칭호 삭제 <칭호번호>§f - 해당 칭호를 삭제합니다.");
					return true;
				}
				RankData rankData = this.loader.getRankByName(args[1]);
				if (!rankData.isExistPrefixToIndex(Integer.parseInt(args[2]))) {
					this.plugin.alert(player, "해당 칭호를 소유하고 있지 §c않습니다 §f!");
					return true;
				}
				String selectedPrefix = rankData.getPrefixByIndex(Integer.parseInt(args[2]));
				rankData.deletePrefixes(new String[] { rankData.getPrefixByIndex(Integer.parseInt(args[2])) });
				if (rankData.getPrefix().equals(selectedPrefix)) {
					double random = Math.random();
					int randomID = (int) (random * (rankData.getPrefixList().size() - 1));
					if (rankData.isExistPrefixToIndex(randomID)) {
						rankData.setPrefix(rankData.getPrefixByIndex(randomID));
						this.provider.applyNameTag(args[1]);
					}
				}
				this.plugin.message(player, "해당 칭호를 §d정상§f적으로 §c삭제§f했습니다 !");
			}
		} else if (string.equals("확인")) {
			if (!(args.length > 1)) {
				this.plugin.message(player, "§d칭호 확인 <유저명> §f- 해당유저의 칭호를 확인합니다.");
				return true;
			}
			RankData rankData = this.loader.getRankByName(args[1]);
			StringBuilder sb = new StringBuilder(TextFormat.DARK_AQUA.toString());
			for(String prefix : rankData.getPrefixList().keySet()) {
				sb
						.append("<")
						.append(prefix)
						.append(">");
			}
			this.plugin.message(player, "해당유저§f가 보유하고 있는 칭호목록을 표시합니다.");
			this.plugin.message(player, sb.toString());
		} else {
			sendHelpMessages(player);
			return true;
		}
		return true;
	}

	public void getPrefixList(Player player, int index) {
		int once_print = 10;

		LinkedHashMap<String, LinkedHashMap<String, Integer>> target = this.loader.getRank(player).getPrefixList();

		int index_count = target.size();
		Object[] index_key = target.keySet().toArray();
		int full_index = (int) Math.floor(index_count / once_print);
		if (index_count > full_index * once_print) {
			full_index++;
		}

		if (index <= full_index) {
			player.sendMessage(new StringBuilder()
					.append(TextFormat.WHITE)
					.append("§l§7《 서버 》§f*보유중인 칭호목록을 표시합니다 !")
					.append(" (")
					.append(index)
					.append("/")
					.append(full_index)
					.append(") 총 : ")
					.append(index_count)
					.append("개").toString());
			player.sendMessage(TextFormat.WHITE + "( §d/칭호 변경 칭호번호§f 또는 §d/칭호 §f를 입력해서 변경 가능합니다 ! )");

			StringBuilder message = new StringBuilder();

			for (int for_i = once_print; for_i >= 1; for_i--) {
				int now_index = index * once_print - for_i;
				if (index_key.length <= now_index)	break;
				String now_key = (String) index_key[now_index];
				message
						.append(TextFormat.WHITE + "[")
						.append(now_index)
						.append("] : ")
						.append(TextFormat.RESET)
						.append(now_key)
						.append("\n");
			}
			player.sendMessage(message.toString());
		} else {
			player.sendMessage(TextFormat.RED + "§c보유하고 있는 칭호가 없습니다 !");
			return;
		}
	}
}
