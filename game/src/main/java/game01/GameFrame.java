package game01;

import com.fasterxml.jackson.databind.ObjectMapper;

import javax.swing.BorderFactory;
import javax.swing.DefaultComboBoxModel;
import javax.swing.DefaultListModel;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JProgressBar;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTextArea;
import javax.swing.ListSelectionModel;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.Timer;
import javax.swing.UIManager;
import java.awt.BorderLayout;
import java.awt.CardLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GridLayout;
import java.awt.Insets;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/** Swing版ダンジョンRPG。元のコンソール版とは別に起動できる。 */
public final class GameFrame extends JFrame {
    private static final String SELECT_SCREEN = "select";
    private static final String BATTLE_SCREEN = "battle";

    private final Random random = new Random();
    private final CardLayout cardLayout = new CardLayout();
    private final JPanel screenPanel = new JPanel(cardLayout);
    private final ObjectMapper mapper = new ObjectMapper();

    private CharacterData[] characterTemplates;
    private CharacterData player;
    private CharacterData enemy;
    private int turnNumber;
    private boolean battleOver;

    private JList<String> characterList;
    private JTextArea characterDetails;
    private JLabel playerNameLabel;
    private JLabel enemyNameLabel;
    private JLabel playerStatsLabel;
    private JLabel enemyStatsLabel;
    private JProgressBar playerHpBar;
    private JProgressBar playerMpBar;
    private JProgressBar enemyHpBar;
    private JProgressBar enemyMpBar;
    private JTextArea battleLog;
    private JComboBox<Skill> skillComboBox;
    private JButton attackButton;
    private JButton skillButton;

    public GameFrame() throws IOException {
        super("ダンジョンRPG - Swing版");
        characterTemplates = loadCharacters();

        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setMinimumSize(new Dimension(780, 600));
        setContentPane(screenPanel);

        screenPanel.add(createSelectionPanel(), SELECT_SCREEN);
        screenPanel.add(createBattlePanel(), BATTLE_SCREEN);

        pack();
        setLocationRelativeTo(null);
        cardLayout.show(screenPanel, SELECT_SCREEN);
    }

    private CharacterData[] loadCharacters() throws IOException {
        try (InputStream input = GameFrame.class.getResourceAsStream("/game01/characters.json")) {
            if (input != null) {
                return mapper.readValue(input, CharacterData[].class);
            }
        }

        // VS Codeからソースを直接実行した場合のフォールバック。
        Path sourceFile = Path.of("src", "main", "java", "game01", "characters.json");
        if (Files.exists(sourceFile)) {
            return mapper.readValue(sourceFile.toFile(), CharacterData[].class);
        }
        throw new IOException("characters.json が見つかりません。");
    }

    private JPanel createSelectionPanel() {
        JPanel panel = new JPanel(new BorderLayout(12, 12));
        panel.setBorder(BorderFactory.createEmptyBorder(18, 18, 18, 18));

        JLabel title = new JLabel("ダンジョンRPG", SwingConstants.CENTER);
        title.setFont(title.getFont().deriveFont(Font.BOLD, 28f));
        panel.add(title, BorderLayout.NORTH);

        DefaultListModel<String> listModel = new DefaultListModel<>();
        for (CharacterData character : characterTemplates) {
            listModel.addElement(String.format(
                    "%s  （HP:%d  MP:%d  ATK:%d  DEF:%d）",
                    character.name, character.hp, character.mp, character.atk, character.def));
        }

        characterList = new JList<>(listModel);
        characterList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        characterList.setFont(characterList.getFont().deriveFont(16f));
        characterList.setFixedCellHeight(34);
        characterList.addListSelectionListener(event -> {
            if (!event.getValueIsAdjusting()) {
                updateCharacterDetails(characterList.getSelectedIndex());
            }
        });

        characterDetails = createReadOnlyTextArea();
        characterDetails.setMargin(new Insets(10, 10, 10, 10));

        JSplitPane splitPane = new JSplitPane(
                JSplitPane.VERTICAL_SPLIT,
                new JScrollPane(characterList),
                new JScrollPane(characterDetails));
        splitPane.setResizeWeight(0.55);
        splitPane.setBorder(BorderFactory.createTitledBorder("キャラクターを選んでください"));
        panel.add(splitPane, BorderLayout.CENTER);

        JButton startButton = new JButton("このキャラクターで開始");
        startButton.setFont(startButton.getFont().deriveFont(Font.BOLD, 16f));
        startButton.addActionListener(event -> startBattle());
        panel.add(startButton, BorderLayout.SOUTH);

        if (characterTemplates.length > 0) {
            characterList.setSelectedIndex(0);
        }
        return panel;
    }

    private void updateCharacterDetails(int selectedIndex) {
        if (selectedIndex < 0) {
            characterDetails.setText("");
            return;
        }

        CharacterData character = characterTemplates[selectedIndex];
        StringBuilder text = new StringBuilder("【スキル】\n");
        if (character.skills == null || character.skills.length == 0) {
            text.append("スキルなし");
        } else {
            for (Skill skill : character.skills) {
                text.append("・").append(skill.name)
                        .append("（消費MP:").append(skill.cost_mp).append("）\n")
                        .append("  ").append(skill.description).append("\n");
            }
        }
        characterDetails.setText(text.toString());
        characterDetails.setCaretPosition(0);
    }

    private JPanel createBattlePanel() {
        JPanel panel = new JPanel(new BorderLayout(12, 12));
        panel.setBorder(BorderFactory.createEmptyBorder(14, 14, 14, 14));

        JPanel statusPanel = new JPanel(new GridLayout(1, 2, 12, 0));
        statusPanel.add(createPlayerStatusPanel());
        statusPanel.add(createEnemyStatusPanel());
        panel.add(statusPanel, BorderLayout.NORTH);

        battleLog = createReadOnlyTextArea();
        battleLog.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 14));
        battleLog.setMargin(new Insets(10, 10, 10, 10));
        JScrollPane logScrollPane = new JScrollPane(battleLog);
        logScrollPane.setBorder(BorderFactory.createTitledBorder("バトルログ"));
        panel.add(logScrollPane, BorderLayout.CENTER);

        JPanel controls = new JPanel(new BorderLayout(8, 8));
        JPanel actions = new JPanel(new GridLayout(1, 3, 8, 0));

        attackButton = new JButton("通常攻撃");
        attackButton.addActionListener(event -> performNormalAttack());

        skillComboBox = new JComboBox<>();

        skillButton = new JButton("スキルを使う");
        skillButton.addActionListener(event -> performSkill());

        actions.add(attackButton);
        actions.add(skillComboBox);
        actions.add(skillButton);

        JButton backButton = new JButton("キャラクター選択へ戻る");
        backButton.addActionListener(event -> returnToSelection());

        controls.add(actions, BorderLayout.CENTER);
        controls.add(backButton, BorderLayout.SOUTH);
        panel.add(controls, BorderLayout.SOUTH);
        return panel;
    }

    private JPanel createPlayerStatusPanel() {
        JPanel panel = createStatusPanel("プレイヤー");
        playerNameLabel = (JLabel) panel.getComponent(0);
        playerHpBar = (JProgressBar) panel.getComponent(1);
        playerMpBar = (JProgressBar) panel.getComponent(2);
        playerStatsLabel = (JLabel) panel.getComponent(3);
        return panel;
    }

    private JPanel createEnemyStatusPanel() {
        JPanel panel = createStatusPanel("敵");
        enemyNameLabel = (JLabel) panel.getComponent(0);
        enemyHpBar = (JProgressBar) panel.getComponent(1);
        enemyMpBar = (JProgressBar) panel.getComponent(2);
        enemyStatsLabel = (JLabel) panel.getComponent(3);
        return panel;
    }

    private JPanel createStatusPanel(String title) {
        JPanel panel = new JPanel(new GridLayout(4, 1, 4, 4));
        panel.setBorder(BorderFactory.createTitledBorder(title));

        JLabel name = new JLabel("-", SwingConstants.CENTER);
        name.setFont(name.getFont().deriveFont(Font.BOLD, 16f));

        JProgressBar hpBar = new JProgressBar();
        hpBar.setStringPainted(true);
        hpBar.setForeground(new Color(65, 170, 90));

        JProgressBar mpBar = new JProgressBar();
        mpBar.setStringPainted(true);
        mpBar.setForeground(new Color(70, 120, 220));

        JLabel stats = new JLabel("ATK: -   DEF: -", SwingConstants.CENTER);

        panel.add(name);
        panel.add(hpBar);
        panel.add(mpBar);
        panel.add(stats);
        return panel;
    }

    private JTextArea createReadOnlyTextArea() {
        JTextArea textArea = new JTextArea();
        textArea.setEditable(false);
        textArea.setLineWrap(true);
        textArea.setWrapStyleWord(true);
        return textArea;
    }

    private void startBattle() {
        int selectedIndex = characterList.getSelectedIndex();
        if (selectedIndex < 0) {
            JOptionPane.showMessageDialog(this, "キャラクターを選択してください。");
            return;
        }
        if (characterTemplates.length < 2) {
            JOptionPane.showMessageDialog(this, "対戦には2体以上のキャラクターが必要です。");
            return;
        }

        player = characterTemplates[selectedIndex].copy();
        int enemyIndex;
        do {
            enemyIndex = random.nextInt(characterTemplates.length);
        } while (enemyIndex == selectedIndex);
        enemy = characterTemplates[enemyIndex].copy();

        turnNumber = 1;
        battleOver = false;
        battleLog.setText("");
        playerHpBar.setMaximum(Math.max(player.hp, 1));
        playerMpBar.setMaximum(Math.max(player.mp, 1));
        enemyHpBar.setMaximum(Math.max(enemy.hp, 1));
        enemyMpBar.setMaximum(Math.max(enemy.mp, 1));
        skillComboBox.setModel(new DefaultComboBoxModel<>(
                player.skills == null ? new Skill[0] : player.skills));

        appendLog("=== バトル開始 ===");
        appendLog(player.name + " vs " + enemy.name);
        appendLog("");
        appendLog("--- " + turnNumber + "ターン目 ---");
        updateStatusDisplay();
        setActionsEnabled(true);
        cardLayout.show(screenPanel, BATTLE_SCREEN);
    }

    private void performNormalAttack() {
        if (battleOver) {
            return;
        }
        setActionsEnabled(false);
        normalAttack(player, enemy);
        finishPlayerAction();
    }

    private void performSkill() {
        if (battleOver) {
            return;
        }
        Skill selectedSkill = (Skill) skillComboBox.getSelectedItem();
        if (selectedSkill == null) {
            appendLog("使用できるスキルがありません。");
            return;
        }
        if (player.mp < selectedSkill.cost_mp) {
            appendLog("MPが足りません。（必要MP:" + selectedSkill.cost_mp + "）");
            return;
        }

        setActionsEnabled(false);
        applySkill(player, enemy, selectedSkill);
        finishPlayerAction();
    }

    private void finishPlayerAction() {
        updateStatusDisplay();
        if (checkGameOver()) {
            return;
        }

        Timer enemyTimer = new Timer(550, event -> performEnemyTurn());
        enemyTimer.setRepeats(false);
        enemyTimer.start();
    }

    private void performEnemyTurn() {
        if (battleOver) {
            return;
        }

        List<Skill> usableSkills = new ArrayList<>();
        if (enemy.skills != null) {
            for (Skill skill : enemy.skills) {
                if (enemy.mp >= skill.cost_mp) {
                    usableSkills.add(skill);
                }
            }
        }

        // スキルを使える場合でも、ときどき通常攻撃を選ぶ。
        if (!usableSkills.isEmpty() && random.nextBoolean()) {
            Skill selectedSkill = usableSkills.get(random.nextInt(usableSkills.size()));
            applySkill(enemy, player, selectedSkill);
        } else {
            normalAttack(enemy, player);
        }

        updateStatusDisplay();
        if (checkGameOver()) {
            return;
        }

        turnNumber++;
        appendLog("");
        appendLog("--- " + turnNumber + "ターン目 ---");
        setActionsEnabled(true);
    }

    private void normalAttack(CharacterData actor, CharacterData target) {
        int damage = Math.max(actor.atk - target.def, 1);
        target.hp = Math.max(0, target.hp - damage);
        appendLog(actor.name + "の通常攻撃！");
        appendLog(target.name + "に" + damage + "ダメージ。");
    }

    private void applySkill(CharacterData actor, CharacterData target, Skill skill) {
        actor.mp = Math.max(0, actor.mp - skill.cost_mp);
        appendLog(actor.name + "は「" + skill.name + "」を使用！");

        applyValue(target, "HP", skill.hp_to_enemy);
        applyValue(target, "MP", skill.mp_to_enemy);
        applyValue(target, "ATK", skill.atk_to_enemy);
        applyValue(target, "DEF", skill.def_to_enemy);
        applyValue(actor, "HP", skill.hp_to_self);
        applyValue(actor, "MP", skill.mp_to_self);
        applyValue(actor, "ATK", skill.atk_to_self);
        applyValue(actor, "DEF", skill.def_to_self);
    }

    private void applyValue(CharacterData target, String stat, int amount) {
        if (amount == 0) {
            return;
        }

        switch (stat) {
            case "HP" -> target.hp = Math.max(0, target.hp + amount);
            case "MP" -> target.mp = Math.max(0, target.mp + amount);
            case "ATK" -> target.atk = Math.max(0, target.atk + amount);
            case "DEF" -> target.def = Math.max(0, target.def + amount);
            default -> throw new IllegalArgumentException("不明なステータス: " + stat);
        }

        String direction = amount > 0 ? "増加" : "減少";
        appendLog(target.name + "の" + stat + "が" + Math.abs(amount) + direction + "。");
    }

    private boolean checkGameOver() {
        if (enemy.hp <= 0) {
            finishBattle("YOU WIN!", "勝利しました！");
            return true;
        }
        if (player.hp <= 0) {
            finishBattle("YOU LOSE...", "敗北しました。");
            return true;
        }
        return false;
    }

    private void finishBattle(String logMessage, String dialogMessage) {
        battleOver = true;
        setActionsEnabled(false);
        appendLog("");
        appendLog("=== " + logMessage + " ===");
        JOptionPane.showMessageDialog(this, dialogMessage, "ゲーム終了", JOptionPane.INFORMATION_MESSAGE);
    }

    private void updateStatusDisplay() {
        updateStatus(player, playerNameLabel, playerHpBar, playerMpBar, playerStatsLabel);
        updateStatus(enemy, enemyNameLabel, enemyHpBar, enemyMpBar, enemyStatsLabel);
    }

    private void updateStatus(
            CharacterData character,
            JLabel nameLabel,
            JProgressBar hpBar,
            JProgressBar mpBar,
            JLabel statsLabel) {
        nameLabel.setText(character.name);
        updateBar(hpBar, character.hp, "HP");
        updateBar(mpBar, character.mp, "MP");
        statsLabel.setText("ATK: " + character.atk + "   DEF: " + character.def);
    }

    private void updateBar(JProgressBar bar, int value, String label) {
        bar.setMaximum(Math.max(Math.max(bar.getMaximum(), value), 1));
        bar.setValue(Math.max(value, 0));
        bar.setString(label + ": " + Math.max(value, 0));
    }

    private void appendLog(String message) {
        battleLog.append(message + System.lineSeparator());
        battleLog.setCaretPosition(battleLog.getDocument().getLength());
    }

    private void setActionsEnabled(boolean enabled) {
        attackButton.setEnabled(enabled && !battleOver);
        skillButton.setEnabled(enabled && !battleOver && skillComboBox.getItemCount() > 0);
        skillComboBox.setEnabled(enabled && !battleOver && skillComboBox.getItemCount() > 0);
    }

    private void returnToSelection() {
        battleOver = true;
        setActionsEnabled(false);
        cardLayout.show(screenPanel, SELECT_SCREEN);
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            try {
                UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
                new GameFrame().setVisible(true);
            } catch (Exception exception) {
                exception.printStackTrace();
                JOptionPane.showMessageDialog(
                        null,
                        "ゲームを起動できませんでした。\n" + exception.getMessage(),
                        "起動エラー",
                        JOptionPane.ERROR_MESSAGE);
            }
        });
    }

    public static final class CharacterData {
        public String name;
        public int hp;
        public int mp;
        public int atk;
        public int def;
        public Skill[] skills;

        public CharacterData() {
        }

        CharacterData copy() {
            CharacterData copy = new CharacterData();
            copy.name = name;
            copy.hp = hp;
            copy.mp = mp;
            copy.atk = atk;
            copy.def = def;
            copy.skills = skills == null ? new Skill[0] : skills.clone();
            return copy;
        }
    }

    public static final class Skill {
        public String name;
        public int cost_mp;
        public int hp_to_enemy;
        public int hp_to_self;
        public int mp_to_enemy;
        public int mp_to_self;
        public int atk_to_enemy;
        public int atk_to_self;
        public int def_to_enemy;
        public int def_to_self;
        public String description;

        public Skill() {
        }

        @Override
        public String toString() {
            return name + "（MP " + cost_mp + "）";
        }
    }
}
