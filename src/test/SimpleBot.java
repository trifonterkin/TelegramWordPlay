package test;

import org.telegram.telegrambots.ApiContextInitializer;
import org.telegram.telegrambots.TelegramBotsApi;
import org.telegram.telegrambots.api.methods.send.SendMessage;
import org.telegram.telegrambots.api.objects.Message;
import org.telegram.telegrambots.api.objects.Update;
import org.telegram.telegrambots.exceptions.TelegramApiException;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;

/**
 * Created by slavon on 03.05.17.
 */
public class SimpleBot extends TelegramLongPollingBot {
    //Буквы, с которых могут ничинаться слова
    static char[] alphabet = {'а','б','в','г','д','е','ж','з','и','й','к','л','м','н','о','п','р','с','т','у','ф','х','ц','ч','ш','щ','э','ю','я'};
    //карта для букв, с которых могут начинаться слова инициализируется методом alphabetFill
    static HashMap<Character, Integer> alphabetMap = new HashMap<>();
    //поле хранящее последнюю букву предыдущего слова, используется для проверки условия - новое слово начинается с последней буквы предыдущего
    static char checkChar = ' ';
    //поле хранящее текущее слово
    static String word;
    //списочный массив словарей. каждый словарь содержит слова на определённую букву
    static ArrayList<HashMap<String,String>> maps = new ArrayList<>();
    //сприсочный массив вхождений в словари. Используется самим ботом для выбора слова
    static ArrayList<Set<Map.Entry<String,String>>> setmap;

    public static void main(String[] args) {
        ApiContextInitializer.init();
        TelegramBotsApi telegramBotsApi = new TelegramBotsApi();
        try {
            telegramBotsApi.registerBot(new SimpleBot());
        } catch (TelegramApiException e) {
            e.printStackTrace();
        }
        //заполнение словарей из файла a.txt методом dictionaryFill
        Path dictPath = Paths.get("a.txt");
        try (Scanner in = new Scanner(dictPath)) {
            dictionaryFill(in, maps);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public String getBotUsername() {
        return "wordPlayBot";
    }

    @Override
    public String getBotToken() {
        return "345668192:AAHlMzE9GwBiTP2G-AzajcT-flafi4lkXJA";
    }

    @Override
    public void onUpdateReceived(Update update) {
        Message message = update.getMessage();
        if (message != null && message.hasText()) {
            {
                word = message.getText();
                //условное выражение для контроля первой итерации.Если итерация первая - проверочный символ становится первым символом введённого пользователем слова
                if (checkChar == ' ') checkChar = extractFirstCharacter(word);
                //происходит проверка введённого слова на корректность первой буквы
                if (checkWord(checkChar, word)) {
                    //происходит проверка наличия введённого слова в словаре
                    if (findWord(word, maps)) {
                        //удаление слова из словаря
                        removeWord(maps,word);
                        //бот выбирает своё слово
                        word = takeBotWord(word);
                        //бот выводит своё слово
                        sendMsg(message,word);
                        //из словаря удаляется слово бота
                        removeWord(maps,word);
                        //запоминается последняя буква слова бота, для проверки следуюшего слова, введённого пользователем
                        checkChar = extractLastCharacter(word);
                    } else {
                        sendMsg(message, "Слово в словаре не найдено");
                    }
                } else {
                    sendMsg(message,"Первая буква не верна");
                }
            }
        }
    }

    private void sendMsg(Message message, String text) {
        SendMessage sendMessage = new SendMessage();
        sendMessage.enableMarkdown(true);
        sendMessage.setChatId(message.getChatId().toString());
        sendMessage.setReplyToMessageId(message.getMessageId());
        sendMessage.setText(text);
        try {
            sendMessage(sendMessage);
        } catch (TelegramApiException e) {
            e.printStackTrace();
        }
    }

    //метод для заполнения словарей из файла
    static void dictionaryFill(Scanner in, ArrayList<HashMap<String ,String>> maps) {
        String tmpString;
        int i = 0;
        long dictionaryLength = 0;
        while (in.hasNextLine()) {
            tmpString = in.nextLine();
            String[] masString = tmpString.split("\\s+");
            maps.add(new HashMap<>());
            /*System.out.println("Заполняется "+i+"й словарь");
            System.out.println("Первое его слово: "+ masString[0]);
            System.out.println("Нумерация в alphabet:" + alphabet[i]);*/
            for (String s :
                    masString) {
                maps.get(i).put(s, s);
                dictionaryLength = dictionaryLength + 1;
            }
            i = i + 1;
        }
        System.out.println("всего слов в словаре  "+ dictionaryLength);
        setmap = MapsToSet(maps);
        alphabetFill(alphabetMap);
    }

    //извлечение последнего символа, который должен стать началом следующего слова
    static char extractLastCharacter(String word) {
        if (!alphabetMap.containsKey(word.charAt(word.length() - 1))) {
            int i = 1;
            while ((i < word.length()-1) & (!alphabetMap.containsKey(word.charAt(word.length() - i)))) {
                i = i + 1;
            }
            return word.charAt(word.length() - i);
        } else {
            return word.charAt(word.length() - 1);
        }
    }
    //извлечение первого символа с защитой от верхнего регистра
    static char extractFirstCharacter(String word) {
        if (word.charAt(0)<'\u0430'& word.charAt(0)>'\u0410') return ((char) (word.charAt(0) + 32));
        else
        return word.charAt(0);
    }
    //перевод первого символа в нижний регистр при необходимости(используется при поиске слова в словаре и удаления из
    static String firstCharacterToLowerCase(String word) {
        if (word.charAt(0) < '\u0430' & word.charAt(0) > '\u0410') {
            StringBuilder builder = new StringBuilder();
            builder.append((char) (word.charAt(0) + 32));
            builder.append(word.substring(1, word.length()));
            return builder.toString();
        }
        else return word;
    }

    //определение номера словаря по символу
    static int findNumberOfDictionary(char letter) {
        return alphabetMap.get(letter);
    }
    //удаление слова из словаря
    static void removeWord(ArrayList<HashMap<String, String>> maps, String word) {
        maps.get(findNumberOfDictionary(extractFirstCharacter(word))).remove(firstCharacterToLowerCase(word));
    }
    //поиск слова в словаре
    static boolean findWord(String word, ArrayList<HashMap<String,String>> maps) {
        return maps.get(findNumberOfDictionary(extractFirstCharacter(word))).containsKey(firstCharacterToLowerCase(word));
    }
    // проверка корректности первого символа
    static boolean checkWord(char firstCharacter, String word) {
        return (firstCharacter == extractFirstCharacter(word));
    }
    // метод для заполнения сприсочного массива вхождений в словари
    static ArrayList<Set<Map.Entry<String,String>>> MapsToSet(ArrayList<HashMap<String,String>> maps) {
        ArrayList<Set<Map.Entry<String, String>>> sets = new ArrayList<>();
        for (HashMap map :
                maps) {
            sets.add(map.entrySet());
        }
        return sets;
    }
    //заполнение карты для букв, с которых могут начинаться слова
    static void alphabetFill(HashMap<Character, Integer> alphabetMap) {
        for (int i = 0; i < alphabet.length; i++) {
            alphabetMap.put(alphabet[i], i);
        }
    }
    //метод для выбора слова ботом
    static String takeBotWord(String oldword) {

        Set<Map.Entry<String, String>> entry = setmap.get(findNumberOfDictionary(extractLastCharacter(oldword)));
        String value = "";
        for (Map.Entry entr :
                entry) {
            value = (String) entr.getValue();
            break;
        }
        return value;
    }
}
