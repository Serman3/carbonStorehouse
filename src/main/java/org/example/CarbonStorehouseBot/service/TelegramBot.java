package org.example.CarbonStorehouseBot.service;

import com.vdurmont.emoji.EmojiParser;
import org.example.CarbonStorehouseBot.config.BotConfig;
import org.example.CarbonStorehouseBot.model.*;
import org.example.CarbonStorehouseBot.repository.ColleagueRepository;
import org.example.CarbonStorehouseBot.repository.FabricRepository;
import org.example.CarbonStorehouseBot.repository.RollRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.api.methods.commands.SetMyCommands;
import org.telegram.telegrambots.meta.api.methods.send.SendDocument;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.methods.updatingmessages.EditMessageText;
import org.telegram.telegrambots.meta.api.objects.InputFile;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.commands.BotCommand;
import org.telegram.telegrambots.meta.api.objects.commands.scope.BotCommandScopeDefault;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.ReplyKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.KeyboardRow;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import java.io.File;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

//@Slf4j
@Component
public class TelegramBot extends TelegramLongPollingBot {

    @Autowired
    private ColleagueRepository colleagueRepository;
    @Autowired
    private RollRepository rollRepository;
    @Autowired
    private FabricRepository fabricRepository;
    @Autowired
    private WriteFile writeFile;
    private final BotConfig botConfig;
    private static final String YES_BUTTON = "YES_BUTTON";
    private static final String NO_BUTTON = "NO_BUTTON";
    private Map<Long, String> userStates = new HashMap<>();
    private static final String START_OUTPUT = EmojiParser.parseToUnicode(" ,добрый день! " + "\n" + "Я бот, который поможет вам c работой по складскому учету" + " :blush:" + "\n" + "Выберети в меню действие."); // EmojiParser and :blush: это смайлик https://emojipedia.org/
    private static final String REGEX_ADD_NEW_BATCH = "[A|А][0-9]{2,3}\\s[0-9]{2,3}\\.[0-9]{2,3}\\s[0-9]{3,4}$";
    private static final String REGEX_DELETE_FABRIC = "[0-9]{2,3}\\.[0-9]{2,3}\\sудалить партию$";
    private static final String REGEX_ADD_NEW_ROLL = "^[0-9]{2,3}\\.[0-9]{2,3}\\s\\d$";
    private static final String REGEX_READY_FABRIC = "[0-9]{1,2}\\.[0-9]{2,3}\\sзавершить$";
    private static final String REGEX_REMARK_ROLL = "^[0-9]{2,3}\\.[0-9]{2,3}\\s\\d\\s[А-Яа-яЁё]*.*";
    private static final String REGEX_READY_ROLL = "^[0-9]{2,3}\\.[0-9]{2,3}\\s\\d\\s[0-9]{2,3}$";
    private static final String REGEX_DELETE_ROLL = "^[0-9]{2,3}\\.[0-9]{2,3}\\s\\d\\sудалить рулон$";
    private static final String REGEX_READY_FABRIC_DATA = "^[0-9]{2,3}\\.[0-9]{2,3}\\s\\А[0-9]{2,3}$";
    private static final String REGEX_NUMBER_FABRIC = "^[0-9]{2,3}\\.[0-9]{2,3}$";
    private static final String HELP_TEXT = "Начинать партию нужно в таком порядке:" + "\n" +
            "1) Когда начинаете новую партию, нажмите в меню кнопку <<addnewbatch>> после этого вводите " +
            "одним предложением через пробел, в формате: название ткани, номер партии, метраж партии А60 23.012 400" + "\n" +
            "2) После этого начинаете делать новый рулон, нажмите в меню кнопку <<addnewroll>>, вводите одним предложением через пробел, в формате: " +
            "номер парти, номер рулона, метраж рулона 23.023 1 55" + "\n" +
            "3) Когда закончили рулон, нажмите в меню кнопку <<readyroll>>, вводите одним предложением через пробел, в формате: " +
            "фактический метраж, номер рулона, номер партии 55 1 23.023" + "\n" +
            "Тоесть каждый раз когда вы начинаете делать новый рулон, вы должны нажимать кнопку <<addnewroll>>, когда вы закничваете, жмете <<readyroll>> " +
            "Когда вы сделали нужное количесво рулонов для партии и внесли все данные в порядке, котрый указан выше, теперь вы можете нажать кнопку <<readyFabric>>, для завершения партии" + "\n" +
            "4) Теперь вы можете завершить партию, для этого нажмите <<readyFabric>>, введите одним предложением через пробел, в формате: номер партии и слово завершить 23.023 завершить" + "\n" +
            "Кнопка <<remarkroll>> для внесения замечаний по рулону, внести замечания вы можете в любой момент (например хвост или какойто косяк в рулоне), одним предложением через пробел, в формате: " +
            "номер руллона, номер партии и текст замечания 1 23.023 текст замечания" + "\n" +
            "Кнопка <<deletefabric>> удаляет партию, возможно вы могли по ошибке внести не правильные данные, введите одним предложением через пробел, в формате: 23.023 удалить партию" + "\n" +
            "Кнопка <<deleteRoll>> удаляет рулон, возможно вы могли по ошибке внести не правильные данные, введите одним предложением через пробел, в формате: 23.023 1 удалить рулон";

    public TelegramBot(BotConfig botConfig) {
        this.botConfig = botConfig;
        List<BotCommand> listOfCommands = new ArrayList<>();
        listOfCommands.add(new BotCommand("/start", "Вступительное сообщение"));
       // listOfCommands.add(new BotCommand("/startworkshift", "Начать новую смену"));
        listOfCommands.add(new BotCommand("/addnewfabric", "Начать новую партию"));
        listOfCommands.add(new BotCommand("/finishfabric", "Закончить партию"));
        listOfCommands.add(new BotCommand("/deletefabric", "Удалить партию"));
        listOfCommands.add(new BotCommand("/addnewroll", "Начать новый руллон"));
        listOfCommands.add(new BotCommand("/remarkroll", "Внести замечания по руллону"));
        listOfCommands.add(new BotCommand("/finishroll", "Закончить рулон"));
        listOfCommands.add(new BotCommand("/deleteroll", "Удалить рулон"));
        listOfCommands.add(new BotCommand("/updatemetricroll", "Обновить метраж руллона"));
        listOfCommands.add(new BotCommand("/marksoldoutfabric", "Пометить партию, как проданную"));
        listOfCommands.add(new BotCommand("/marksoldoutroll", "Пометить руллон, как проданный"));
        listOfCommands.add(new BotCommand("/remainder", "Остаток по ткани"));
        listOfCommands.add(new BotCommand("/readyfabricdata", "Посмотреть готовые партии"));
        listOfCommands.add(new BotCommand("/help", "Информация по боту"));
        try {
            this.execute(new SetMyCommands(listOfCommands, new BotCommandScopeDefault(), null));
        }catch (TelegramApiException e){
            e.printStackTrace();
            //log.error("Error setting bot command list " + e.getMessage());
        }
    }

    @Override
    public String getBotUsername() {
        return botConfig.getBotName();
    }

    @Override
    public String getBotToken() {
        return botConfig.getToken();
    }

    @Override
    public void onUpdateReceived(Update update) {
        if(update.hasMessage() && update.getMessage().hasText()){
            long chatId = update.getMessage().getChatId();
            String messageText = update.getMessage().getText();

            String state = getState(chatId);

            if(messageText.equals("/start")){
                sendMessage(chatId, update.getMessage().getChat().getFirstName() + START_OUTPUT);
                saveColleague(update.getMessage());
            }

            //--------Работа с партией-------------
            if(messageText.equals("/addnewfabric")){
                //questionStatusFabric(chatId);
                sendMessage(chatId, "Для добавления новой партии ведите название ткани, номер партии, метраж партии одним предложением через пробел, в формате: А60 23.023 400");
                setState(chatId, "ADD_NEW_BATCH");
            }
            if(/*messageText.matches(REGEX_ADD_NEW_BATCH) && */state.equals("ADD_NEW_BATCH")){
                saveNewFabric(chatId, messageText);
                deleteState();
            }
            if(messageText.equals("/deletefabric")){
                sendMessage(chatId, "Для удаления партии ведите номер партии, в формате: 23.023");
                setState(chatId, "DELETE_FABRIC");
            }
            if(/*messageText.matches(REGEX_DELETE_FABRIC) && */state.equals("DELETE_FABRIC")){
                deleteFabric(chatId, messageText);
                deleteState();
            }
            if(messageText.equals("/finishfabric")){
                sendMessage(chatId, "Чтобы законить партию введите номер партии, в формате: 23.023");
                setState(chatId, "READY_FABRIC");
            }
            if(/*messageText.matches(REGEX_READY_FABRIC) && */state.equals("READY_FABRIC")){
                finishTheFabric(chatId, messageText);
                deleteState();
            }
            if(messageText.equals("/marksoldoutfabric")){
                sendMessage(chatId, "Чтобы пометить партию как проданную, введите номер партии, в формате: 23.023");
                setState(chatId, "MARK_SOLD_OUT_FABRIC");
            }
            if(state.equals("MARK_SOLD_OUT_FABRIC")){
                updateStatusFabricSoldOut(chatId, messageText);
                deleteState();
            }

            //--------Работа с руллоном-------------
            if(messageText.equals("/addnewroll")){
                sendMessage(chatId, "Для добавления рулона введите номер парти, номер рулона одним предложением через пробел, в формате: 23.023 1");
                setState(chatId, "ADD_NEW_ROLL");
            }
            if(/*messageText.matches(REGEX_ADD_NEW_ROLL) && */state.equals("ADD_NEW_ROLL")){
                saveNewRoll(messageText,chatId);
                deleteState();
            }
            if(messageText.equals("/remarkroll")){
                sendMessage(chatId, "Для внесения замечания по рулону введите номер партии, номер рулона и текст замечания одним предложением через пробел, в формате: 23.023 1 текст замечания");
                setState(chatId, "REMARK_ROLL");
            }
            if(/*messageText.matches(REGEX_REMARK_ROLL) && */state.equals("REMARK_ROLL")){
                updateRemarkRoll(chatId, messageText);
                deleteState();
            }
            if(messageText.equals("/finishroll")){
                sendMessage(chatId, "Чтобы закончить рулон введите номер партии, номер рулона, фактический метраж одним предложеием через пробел, в формате: 23.023 1 55");
                setState(chatId, "READY_ROLL");
            }
            if(/*messageText.matches(REGEX_READY_ROLL) && */state.equals("READY_ROLL")){
                finishRoll(chatId, messageText);
                deleteState();
            }
            if(messageText.equals("/deleteroll")){
                sendMessage(chatId, "Чтобы удалить рулон введите номер партии, номер рулона одним предложение через пробел, в формате: 23.023 1");
                setState(chatId, "DELETE_ROLL");
            }
            if(/*messageText.matches(REGEX_DELETE_ROLL) && */state.equals("DELETE_ROLL")){
                deleteRoll(chatId, messageText);
                deleteState();
            }

            //--------Вывод остатков-------------
            if(messageText.equals("/remainder")){
                SendMessage sendMessage = sendMessageFromFabricKeyboardNameFabric(new SendMessage(String.valueOf(chatId), "Выберите по какой ткани показать остаток"));
                executeMessage(sendMessage);
            }
            if(getReplacedNameFabric(messageText).equals("А40")){
                writeFile.writeExcelFileReadyFabric(getRemainderFabrics(chatId, messageText), messageText);
                sendDocument(chatId, "C:/doc/" + messageText + ".xlsx");
            }
            if(getReplacedNameFabric(messageText).equals("А60")){
                writeFile.writeExcelFileReadyFabric(getRemainderFabrics(chatId, messageText), messageText);
                sendDocument(chatId, "C:/doc/" + messageText + ".xlsx");
            }
            if(getReplacedNameFabric(messageText).equals("А80")){
                writeFile.writeExcelFileReadyFabric(getRemainderFabrics(chatId, messageText), messageText);
                sendDocument(chatId, "C:/doc/" + messageText + ".xlsx");
            }
            if(getReplacedNameFabric(messageText).equals("А120")){
                writeFile.writeExcelFileReadyFabric(getRemainderFabrics(chatId, messageText), messageText);
                sendDocument(chatId, "C:/doc/" + messageText + ".xlsx");
            }
            if(getReplacedNameFabric(messageText).equals("А160")){
                writeFile.writeExcelFileReadyFabric(getRemainderFabrics(chatId, messageText), messageText);
                sendDocument(chatId, "C:/doc/" + messageText + ".xlsx");
            }

            //--------Вывод готовых партий-------------
            if(messageText.equals("/readyfabricdata")){
                SendMessage sendMessage = sendMessageFromFabricKeyboardNumberFabric(new SendMessage(String.valueOf(chatId), "Выберите по какой партии показать данные"));
                executeMessage(sendMessage);
            }
            if(messageText.matches(REGEX_READY_FABRIC_DATA)){
                writeFile.writeExcelFileReadyFabric(getOneReadyFabric(chatId, messageText), messageText);
                sendDocument(chatId, "C:/doc/" + messageText + ".xlsx");
            }if(messageText.equals("Все готовые партии")){
                writeFile.writeExcelFileReadyFabric(getAllReadyFabric(chatId), messageText);
                sendDocument(chatId, "C:/doc/" + messageText + ".xlsx");
            }

           /* if (messageText.equals("/startworkshift")){
                sendMessage(chatId, "Чтобы начать смену введите номер партии, номер руллона одним предложение через пробел, в формате: 23.023 1");
                setState(chatId, "START_WORK_SHIFT");
            }
            if(state.equals("START_WORK_SHIFT")){
                startWorkShift(chatId, messageText);
                deleteState();
            }*/

            //--------help-------------
            if(messageText.equals("/help")){
                sendMessage(chatId, HELP_TEXT);
            }
        }else if(update.hasCallbackQuery()){
            String callbackData = update.getCallbackQuery().getData();
            long messageId = update.getCallbackQuery().getMessage().getMessageId();
            long chatId = update.getCallbackQuery().getMessage().getChatId();

            if(callbackData.equals(YES_BUTTON)){
                String text = "Создаем новую партию";
                executeEditMessageText(text, chatId, messageId);
            }
            else if(callbackData.equals(NO_BUTTON)){
                String text = "Рабботаем с имеющейся на складе партией";
                executeEditMessageText(text, chatId, messageId);
            }
        }
    }

    private void setState(long chatId, String state){
        userStates.put(chatId, state);
    }

    private String getState(long chatId){
        return userStates.getOrDefault(chatId, "/start");
    }

    private void deleteState(){
        userStates.clear();
    }


    //--------------COLLEAGUE-----------------------------COLLEAGUE-----------
    private void saveColleague(Message message){
        if(colleagueRepository.findById(message.getChatId()).isEmpty()) {
            var chatId = message.getChatId();
            var chat = message.getChat();
            Colleague colleague = new Colleague();
            colleague.setChatId(chatId);
            colleague.setFirstName(chat.getFirstName());
            colleague.setLastName(chat.getLastName());
            colleague.setUserName(chat.getUserName());
            colleague.setDateRegister(LocalDateTime.now());
            colleagueRepository.save(colleague);
            System.out.println("Colleague save " + colleague);
            //log.info("Colleague save " + colleague);
        }
    }


    //--------------FABRIC----------------------------------FABRIC----------
    private void saveNewFabric(long chatId, String messageText){
        if(messageText.matches(REGEX_ADD_NEW_BATCH)){
            String[] data = messageText.split("\s");
            if(fabricRepository.findById(data[1]).isEmpty()){
                Colleague colleague = colleagueRepository.findById(chatId).get();
                Fabric fabric = new Fabric();
                fabric.setBatchNumberId(data[1]);
                fabric.setDateManufacture(LocalDateTime.now());
                fabric.setMetricAreaBatch(Integer.parseInt(data[2]));
                fabric.setNameFabric(getReplacedNameFabric(data[0]));
                fabric.addColleague(colleague);
                fabric.setStatusFabric(StatusFabric.AT_WORK);
                fabricRepository.save(fabric);
                System.out.println(colleague);
                sendMessage(chatId, "Вы начали делать новую партию " + messageText);
            }else{
                sendMessage(chatId, "Такая партия уже сущесвует");
            }
        }else{
            sendMessage(chatId, "Не удается сохранить новую партию, неверный формат ввода");
        }
    }

    private void finishTheFabric(long chatId, String messageText){
        if(messageText.matches(REGEX_NUMBER_FABRIC)){
            Fabric fabric = fabricRepository.findById(messageText).orElse(null);
            if(fabric != null){
                fabric.setStatusFabric(StatusFabric.READY);
                fabric.setDateManufacture(LocalDateTime.now());
                fabricRepository.save(fabric);
                sendMessage(chatId, "Партия с номером " + messageText + " завершена");
            }else{
                sendMessage(chatId, "Партии с номером " + messageText + " нет в базе данных для завершения");
            }
        }else{
            sendMessage(chatId, "Не удается закончить партию, неверный формат ввода");
        }
    }

    @Transactional
    private void deleteFabric(long chatId, String messageText){
        if(messageText.matches(REGEX_NUMBER_FABRIC)){
            Fabric fabric = fabricRepository.findById(messageText).orElse(null);
            if (fabric != null){
                rollRepository.deleteAllByFabricId(fabric.getBatchNumberId());
                fabric.remove();
                fabricRepository.deleteFabric(fabric.getBatchNumberId());
                sendMessage(chatId, "Партия с номером " + messageText + " была удалена");
            }
            else {
                sendMessage(chatId, "Партии с номером " + messageText + " нет в базе данных для удаления");
            }
        }else {
            sendMessage(chatId, "Не удается удалить партию, неверный формат ввода");
        }
    }

    private void updateStatusFabricSoldOut(long chatId, String messageText){
        if(messageText.matches(REGEX_NUMBER_FABRIC)){
            Fabric fabric = fabricRepository.findById(messageText).orElse(null);
            if(fabric != null){
                fabricRepository.updateStatusFabric(StatusFabric.SOLD_OUT.toString(), fabric.getBatchNumberId());
                rollRepository.updateStatusRoll(StatusRoll.SOLD_OUT.toString(), fabric.getBatchNumberId());
                sendMessage(chatId, "Статус партии " + fabric.getBatchNumberId() + " изменен на продано");
            }else {
                sendMessage(chatId, "Такой партии не сущесвует");
            }
        }else{
            sendMessage(chatId, "Не удается обновить статус партии на продано, неверный формат ввода");
        }
    }

    private Map<List<Object[]>, List<Roll>> getOneReadyFabric(long chatId, String messageText){
        String[] data = messageText.split("\s");
        Map<List<Object[]>, List<Roll>> allDataFabric = new HashMap<>();
        List<Object[]> infoFabrics = fabricRepository.allInfoFabricAndSumMetricArea(data[0]);
        List<Roll> rollLists = rollRepository.findByFabricId(data[0]);
        if(infoFabrics.get(0) != null && !rollLists.isEmpty()){
            allDataFabric.put(infoFabrics, rollLists);
        }else {
            sendMessage(chatId, "Партия " + data[0] + " находится в работе, по ней нет готовых рулонов");
        }
        return allDataFabric;
    }

    private Map<List<Object[]>, List<Roll>> getAllReadyFabric(long chatId) {
        Map<List<Object[]>, List<Roll>> allDataFabric = new HashMap<>();
        List<Fabric> fabrics = fabricRepository.findByAllStatusFabricReady(StatusFabric.READY.toString());
        for (Fabric fabric : fabrics) {
            List<Object[]> infoFabrics = fabricRepository.allInfoFabricAndSumMetricArea(fabric.getBatchNumberId());
            List<Roll> rollLists = rollRepository.findByFabricId(fabric.getBatchNumberId());
            if(infoFabrics.get(0) != null && !rollLists.isEmpty()){
                allDataFabric.put(infoFabrics, rollLists);
            }else {
                sendMessage(chatId, "Партия " + fabric.getBatchNumberId() + " находится в работе, по ней нет готовых рулонов");
            }
        }
        return allDataFabric;
    }

    private Map<List<Object[]>, List<Roll>> getRemainderFabrics(long chatId, String messageText){
        Map<List<Object[]>, List<Roll>> allDataFabric = new HashMap<>();
        List<Fabric> fabricList = fabricRepository.findByNameFabric(getReplacedNameFabric(messageText));
        if(fabricList.isEmpty()){
            sendMessage(chatId, "Остатков по ткани " + messageText + " на складе нет");
        }else {
            for(Fabric fabric : fabricList){
                List<Object[]> allInfo = fabricRepository.allInfoFabricAndSumMetricArea(fabric.getBatchNumberId());
                List<Roll> allRoll = rollRepository.findByFabricId(fabric.getBatchNumberId());
                if(allInfo.get(0) != null && !allRoll.isEmpty()){
                    allDataFabric.put(allInfo, allRoll);
                }else {
                    sendMessage(chatId, "Партия " + fabric.getBatchNumberId() + " находится в работе, по ней нет готовых рулонов");
                }
            }
        }
        return allDataFabric;
    }
/*
    private void startWorkShift(long chatId, String messageText){
        String[] data = messageText.split("\s");
        Fabric fabric = fabricRepository.findById(data[0]).orElse(null);
        Colleague colleague = colleagueRepository.findById(chatId).orElse(null);
        if(fabric != null){
            Roll roll = rollRepository.findByNumberRollAndFabricId(Integer.parseInt(data[1]), fabric.getBatchNumberId()).orElse(null);
            if(roll == null){
                Roll newRoll = new Roll();
                newRoll.setNumberRoll(Integer.parseInt(data[1]));
                newRoll.setRollMetric(0);
                newRoll.setDateFulfilment(LocalDateTime.now());
                newRoll.setStatusRoll(StatusRoll.AT_WORK);
                newRoll.setRemark(null);
                newRoll.setFabric(fabric);
                newRoll.addColleague(colleague);
                rollRepository.save(newRoll);
                sendMessage(chatId, colleague.getFirstName() + " начал делать новый рулон под номером " + data[1] + " для партии " + data[0]);
            }else{
                roll.addColleague(colleague);
                sendMessage(chatId, "Вы продолжаете делать рулон");
            }
        }else {
            sendMessage(chatId, "Для добавления новой партии нажмите кнопку в меню /addnewbatch");
        }
    }*/


    //--------------ROLL------------------------------------ROLL----------
    private void saveNewRoll(String messageText, long chatId){
        if(messageText.matches(REGEX_ADD_NEW_ROLL)){
            String[] data = messageText.split("\s");
            Fabric fabric = fabricRepository.findById(data[0]).orElse(null);
            if(fabric != null && rollRepository.findByNumberRollAndFabricId(Integer.parseInt(data[1]), data[0]).isEmpty()){
                Roll roll = new Roll();
                roll.setNumberRoll(Integer.parseInt(data[1]));
                roll.setRollMetric(0);
                roll.setDateFulfilment(LocalDateTime.now());
                roll.setStatusRoll(StatusRoll.AT_WORK);
                roll.setRemark(null);
                roll.setFabric(fabric);
                rollRepository.save(roll);
                sendMessage(chatId, "Руллон под номером " + data[1] + " добавлен к партии " + data[0]);
            }else {
                sendMessage(chatId, "Не удается добавить рулон, вы ввели несуществующий номер партии или рулон под номером "+ data[1] + " уже сущесвует");
            }
        }else{
            sendMessage(chatId, "Не удается сохранить новый руллон, неверный формат ввода");
        }
    }

    private void updateRemarkRoll(long chatId, String messageText){
        if(messageText.matches(REGEX_REMARK_ROLL)){
            String[] data = messageText.split("\s");
            String numberFabricAndRoll = data[0] + " " + data[1] + " ";
            String remark = messageText.replaceFirst(numberFabricAndRoll, "");
            Fabric fabric = fabricRepository.findById(data[0]).orElse(null);
            if(fabric != null){
                Roll roll = rollRepository.findByNumberRollAndFabricId(Integer.parseInt(data[1]), data[0]).get();
                roll.setRemark(remark);
                roll.setDateFulfilment(LocalDateTime.now());
                rollRepository.save(roll);
                sendMessage(chatId, "Замечания по рулону " + data[1] + " в партии " + data[0] + " внесены");
            }else{
                sendMessage(chatId, "Партии с номером " + data[0] + " нет в базе данных для внесения замечания по рулону");
            }
        }else {
            sendMessage(chatId, "Не удается внести замечания по рулону, неверный формат ввода");
        }
    }

    private void finishRoll(long chatId, String messageText){
        if (messageText.matches(REGEX_READY_ROLL)) {
            String[] data = messageText.split("\s");
            Roll roll = rollRepository.findByNumberRollAndFabricId(Integer.parseInt(data[1]), data[0]).orElse(null);
            if(roll != null){
                roll.setStatusRoll(StatusRoll.READY);
                roll.setRollMetric(Integer.parseInt(data[2]));
                roll.setDateFulfilment(LocalDateTime.now());
                rollRepository.save(roll);
                sendMessage(chatId, "Рулон с номером " + data[1] + " для партии " + data[0] +  " завершен");
            }else{
                sendMessage(chatId, "Рулона с номером " + data[1] + " нет в базе данных для завершения");
            }
        }else {
            sendMessage(chatId, "Не удается закончить рулон, неверный формат ввода");
        }
    }

    @Transactional
    private void deleteRoll(long chatId, String messageText){
        if(messageText.matches(REGEX_ADD_NEW_ROLL)){
            String[] data = messageText.split("\s");
            Roll roll = rollRepository.findByNumberRollAndFabricId(Integer.parseInt(data[1]), data[0]).orElse(null);
            if(roll != null){
                /*roll.remove();
                rollRepository.deleteFromRollAndColleague(roll.getId());*/
                rollRepository.deleteByFabricIdAndNumberRoll(data[0], Integer.parseInt(data[1]));
                sendMessage(chatId, "Рулон под номером " + data[1] + " ,был удален у партии " + data[0]);
            }else{
                sendMessage(chatId, "Рулона с номером " + data[1] + " нет в базе данных для завершения");
            }
        }else{
            sendMessage(chatId, "Не удается удалить рулон, неверный формат ввода");
        }
    }


    //--------------SEND_MESSAGE-----------------------------SEND_MESSAGE---------
    private void sendMessage(long chatId, String textToSend) {
        SendMessage sendMessage = new SendMessage();
        sendMessage.setChatId(String.valueOf(chatId));
        sendMessage.setText(textToSend);
        executeMessage(sendMessage);
    }

    private void sendDocument(Long chatId, String filePath) {
        File file = new File(filePath);
        SendDocument sendDocument = new SendDocument();
        sendDocument.setChatId(String.valueOf(chatId));
        sendDocument.setDocument(new InputFile(file));
        try {
            execute(sendDocument);
        } catch (TelegramApiException e) {
            e.printStackTrace();
        }
    }

    private SendMessage sendMessageFromFabricKeyboardNumberFabric(SendMessage sendMessage){
        List<Fabric> fabricList = fabricRepository.findByAllStatusFabricReady(StatusFabric.READY.toString());
        ReplyKeyboardMarkup replyKeyboardMarkup = new ReplyKeyboardMarkup();
        List<KeyboardRow> keyboardRows = new ArrayList<>();
        KeyboardRow row = new KeyboardRow();
        if(fabricList.size() >= 5) {
            for (int i = 0; i < fabricList.size(); i++) {
                row.add(fabricList.get(i).getBatchNumberId() + " " + fabricList.get(i).getNameFabric());
                if ((i + 1) % 5 == 0) {
                    keyboardRows.add(row);
                    row = new KeyboardRow();
                }
            }
        }else {
           for (int i = 0; i < fabricList.size(); i++){
               row.add(fabricList.get(i).getBatchNumberId() + " " + fabricList.get(i).getNameFabric());
           }
        }
        row.add("Все готовые партии");
        replyKeyboardMarkup.setKeyboard(keyboardRows);
        sendMessage.setReplyMarkup(replyKeyboardMarkup);
        keyboardRows.add(row);
        replyKeyboardMarkup.setKeyboard(keyboardRows);
        sendMessage.setReplyMarkup(replyKeyboardMarkup);
        return sendMessage;
    }

    public SendMessage sendMessageFromFabricKeyboardNameFabric(SendMessage sendMessage){
        ReplyKeyboardMarkup replyKeyboardMarkup = new ReplyKeyboardMarkup();
        List<KeyboardRow> keyboardRows = new ArrayList<>();
        KeyboardRow row = new KeyboardRow();
        row.add("А40");
        row.add("А60");
        keyboardRows.add(row);
        row = new KeyboardRow();
        row.add("А80");
        row.add("А120");
        row.add("А160");
        keyboardRows.add(row);
        replyKeyboardMarkup.setKeyboard(keyboardRows);
        sendMessage.setReplyMarkup(replyKeyboardMarkup);
        return sendMessage;
    }


    //--------------EXECUTE_MESSAGE-----------------------EXECUTE_MESSAGE------------
    private void executeEditMessageText(String text, long chatId, long messageId){
        EditMessageText message = new EditMessageText();
        message.setChatId(String.valueOf(chatId));
        message.setText(text);
        message.setMessageId((int) messageId);
        try {
            execute(message);
        } catch (TelegramApiException e) {
            e.printStackTrace();
            //log.error(ERROR_TEXT + e.getMessage());
        }
    }

    private void executeMessage(SendMessage message){
        try {
            execute(message);
        } catch (TelegramApiException e) {
            e.printStackTrace();
            //log.error(ERROR_TEXT + e.getMessage());
        }
    }

    private void questionStatusFabric(long chatId){
        SendMessage sendMessage = new SendMessage();
        sendMessage.setChatId(String.valueOf(chatId));
        sendMessage.setText("Новая партия?");

        InlineKeyboardMarkup markupInLine = new InlineKeyboardMarkup();
        List<List<InlineKeyboardButton>> rowsInLine = new ArrayList<>();
        List<InlineKeyboardButton> rowInLine = new ArrayList<>();
        var yesButton = new InlineKeyboardButton();

        yesButton.setText("Да");
        yesButton.setCallbackData(YES_BUTTON);

        var noButton = new InlineKeyboardButton();

        noButton.setText("Нет");
        noButton.setCallbackData(NO_BUTTON);

        rowInLine.add(yesButton);
        rowInLine.add(noButton);

        rowsInLine.add(rowInLine);

        markupInLine.setKeyboard(rowsInLine);
        sendMessage.setReplyMarkup(markupInLine);

        executeMessage(sendMessage);
    }

    private String getReplacedNameFabric(String messageText){
        String nameFabric = "";
        if(messageText.contains("A")){
            nameFabric = messageText.replaceFirst("A", "А");
            return nameFabric;
        }
        return messageText;
    }

}
