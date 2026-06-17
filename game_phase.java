
import java.util.Random;
import java.util.Scanner;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.File;

class game_phase {

    static Scanner sc = new Scanner(System.in);

    public static void main(String args[]) throws Exception {

        // jsonファイルのインポート
        ObjectMapper mapper = new ObjectMapper();
        File file = new File("C:\\Users\\81704\\Documents\\Java\\game\\game\\src\\main\\java\\characters.json");
        character[] charalist = mapper.readValue(file, character[].class);

        // ゲーム初期画面
        System.out.println("===ダンジョンRPG===");
        System.out.println("キャラクターを選んでください");
        System.out.println("----------------------------------");

        // キャラリスト表示
        for (int i = 0; i < charalist.length; i++) {
            System.out.print(i + ".");
            plint_status(charalist[i]);
        }
        System.out.println("----------------------------------");

        // ユーザキャラ選択
        int charanum = sc.nextInt();

        // 敵キャラ選択
        Random random = new Random();
        int randomInt;
        do {
            randomInt = random.nextInt(charalist.length);
        } while (randomInt == charanum);

        // ユーザ、敵キャラの反映
        character user = charalist[charanum];
        user.setName("あなた");
        character computer = charalist[randomInt];

        // 各ターン開始
        turn_phase(user, computer);
    }

    static void turn_phase(character player, character enemy) {
        for (int i = 1; true; i++) {
            System.out.println("===" + i + "ターン目===");

            // ステータス表示
            plint_status(player);
            plint_status(enemy);

            // 行動選択
            System.out.println("===PC行動選択===");
            System.out.println("0.通常攻撃");
            System.out.println("1.スキルを使う");

            int act = sc.nextInt();

            // 通常攻撃選択時
            if (act == 0) {
                nomal_action(player, enemy);
            }
            // スキル攻撃選択
            else if (act == 1) {
                System.out.println("----------------------------------");
                for (int j = 0; j < player.skills.length; j++) {
                    System.out.print(j + "." + player.skills[j].name);
                    System.out.print("  (消費MP:" + player.skills[j].cost_mp + ")  ");
                    System.out.println(player.skills[j].description);
                }
                System.out.println("----------------------------------");

                int skillnum = sc.nextInt();
                skil_action(player, enemy, skillnum);
            }

            // 勝敗判定
            judge_game(player, enemy);
            // 敵の攻撃
            Random random = new Random();
            int enemyattck;
            do {
                enemyattck = random.nextInt(enemy.skills.length + 1);
                if (enemyattck == enemy.skills.length) {
                    break;
                }
            } while (enemy.mp >= enemy.skills[enemyattck].cost_mp);

            if (enemyattck == enemy.skills.length) {
                nomal_action(enemy, player);
            } else {
                skil_action(enemy, player, enemyattck);
            }

            // 勝敗判定
            judge_game(player, enemy);
        }

    }

    // ステータス表示用
    static void plint_status(character player) {
        System.out.print("[" + player.name + "  ");
        System.out.print(" HP:" + player.hp);
        System.out.print(" MP:" + player.mp);
        System.out.print(" ATK:" + player.atk);
        System.out.print(" DEF:" + player.def);
        System.out.println("]");
    }

    // 通常攻撃
    static void nomal_action(character player, character enemy) {
        System.out.println(player.name + "の通常攻撃!");
        int atk = player.atk;
        int def = enemy.def;
        int damage = Math.max(atk - def, 1);
        enemy.hp -= damage;
        System.out.println(enemy.name + "に" + damage + "のダメージを与えた");
        System.out.println("----------------------------------");
    }

    // スキル攻撃
    static void skil_action(character player, character enemy, int num) {
        if (player.mp < player.skills[num].cost_mp) {
            System.out.println("MPが足りなかった");
            return;
        }
        System.out.println(player.name + "は" + player.skills[num].name + "を使用した！");
        player.mp -= player.skills[num].cost_mp;
        if (player.skills[num].hp_to_enemy > 0) {
            enemy.hp -= player.skills[num].hp_to_enemy;
            System.out.println(enemy.name + "に" + player.skills[num].hp_to_enemy + "のダメージを与えた");
        } else if (player.skills[num].hp_to_enemy < 0) {
            enemy.hp -= player.skills[num].hp_to_enemy;
            System.out.println(enemy.name + "が" + player.skills[num].hp_to_enemy + "回復した");
        }

        if (player.skills[num].mp_to_enemy > 0) {
            enemy.mp -= player.skills[num].mp_to_enemy;
            System.out.println(enemy.name + "に" + player.skills[num].mp_to_enemy + "のMPを減らした");
        } else if (player.skills[num].mp_to_enemy < 0) {
            enemy.mp -= player.skills[num].mp_to_enemy;
            System.out.println(enemy.name + "が" + player.skills[num].mp_to_enemy + "のMPを回復した");
        }

        if (player.skills[num].atk_to_enemy > 0) {
            enemy.atk -= player.skills[num].atk_to_enemy;
            System.out.println(enemy.name + "に" + player.skills[num].atk_to_enemy + "の攻撃力を下げた");
        }

        if (player.skills[num].def_to_enemy > 0) {
            enemy.def -= player.skills[num].def_to_enemy;
            System.out.println(enemy.name + "に" + player.skills[num].def_to_enemy + "の防御力を下げた");
        }

        if (player.skills[num].hp_to_self > 0) {
            player.hp += player.skills[num].hp_to_self;
            System.out.println(player.name + "のHPが" + player.skills[num].hp_to_self + "回復した");
        } else if (player.skills[num].hp_to_self < 0) {
            player.hp += player.skills[num].hp_to_self;
            System.out.println(player.name + "のHPが" + player.skills[num].hp_to_self + "減った");
        }

        if (player.skills[num].mp_to_self > 0) {
            player.mp += player.skills[num].mp_to_self;
            System.out.println(player.name + "のMPが" + player.skills[num].hp_to_self + "回復した");
        } else if (player.skills[num].mp_to_self < 0) {
            player.mp += player.skills[num].mp_to_self;
            System.out.println(player.name + "のMPが" + player.skills[num].mp_to_self + "減った");
        }

        if (player.skills[num].atk_to_self > 0) {
            player.atk += player.skills[num].atk_to_self;
            System.out.println(player.name + "の攻撃力が" + player.skills[num].atk_to_self + "上がった");
        }

        if (player.skills[num].def_to_self > 0) {
            player.def += player.skills[num].def_to_self;
            System.out.println(player.name + "の防御力が" + player.skills[num].def_to_self + "上がった");
        }

        System.out.println("----------------------------------");

    }

    // 勝敗判定
    static void judge_game(character player, character enemy) {
        if (player.hp <= 0) {
            System.out.println("===YOU LOSE===");
            System.exit(0);
        } else if (enemy.hp <= 0) {
            System.out.println("===YOU WIN===");
            System.exit(0);
        }
    }

    // キャラクタクラス
    public static class character {
        public String name;
        public int hp;
        public int mp;
        public int atk;
        public int def;
        public skill[] skills;

        public character() {
        }

        public void setName(String name) {
            this.name = name;
        }
    }

    // スキルクラス
    public static class skill {
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

        public skill() {
        }
    };

}