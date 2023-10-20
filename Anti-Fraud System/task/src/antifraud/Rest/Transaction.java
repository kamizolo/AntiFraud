package antifraud.Rest;

import antifraud.Card.ActiveCard;
import antifraud.Card.ActiveCardRepository;
import antifraud.Card.CardRepository;
import antifraud.Card.StolenCard;
import antifraud.Ip.IpRepository;
import antifraud.Ip.SuspiciousIp;
import antifraud.Transfer.Transfer;
import antifraud.Transfer.TransferRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.commons.validator.routines.InetAddressValidator;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.apache.commons.validator.routines.checkdigit.LuhnCheckDigit;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;


import javax.transaction.Transactional;
import javax.validation.Valid;
import javax.validation.constraints.NotBlank;
import java.time.LocalDateTime;
import java.util.*;

@Controller
public class Transaction {

    @Autowired
    IpRepository ipRepository;
    @Autowired
    CardRepository cardRepository;
    @Autowired
    TransferRepository transferRepository;
    @Autowired
    ActiveCardRepository activeCardRepository;
    String result = "";

    //checks how to handle a transaction amount
    public String test(long amount, int allowedMax, int manualMax) {
        if (amount <= 0) {
            return "BAD REQUEST";
        } else if (amount <= allowedMax) {
            return "ALLOWED";
        } else if (amount <= manualMax) {
            return "MANUAL_PROCESSING";
        } else {
            return "PROHIBITED";
        }
    }
    //returns list of all transactions
    @GetMapping(value = "/api/antifraud/history")
    public ResponseEntity<String> transactionHistory () throws JsonProcessingException {
        return new ResponseEntity<>(listOfObject(transferRepository.findAllByOrderByIdAsc()),HttpStatus.OK);
    }
    //returns list of all transactions for a card
    @GetMapping(value = "/api/antifraud/history/{number}")
    public ResponseEntity<String> transactionCardHistory (@PathVariable String number) throws JsonProcessingException {
        List<Transfer> transfers = transferRepository.findTransferByCardNumberOrderByIdAsc(number);
        //check if number is valid
        String regex = "^(\\d{16})$";
        if (!number.matches(regex) || !LuhnCheckDigit.LUHN_CHECK_DIGIT.isValid(number)) {
            return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
        }
        //checks if card is in db
        if (transfers.isEmpty()) {
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        }
        return new ResponseEntity<>(listOfObject(transfers),HttpStatus.OK);
    }
    //updates transaction with feedback and changes max transaction values based on that
    @PutMapping(value = "/api/antifraud/transaction")
    public ResponseEntity<String> transactionFeedback (@RequestBody feedback request) throws JsonProcessingException {

        //acceptable statuses
        List<String> statuses = List.of("ALLOWED","MANUAL_PROCESSING","PROHIBITED");
        //tests if status is valid
        if(!statuses.contains(request.feedback)){
            return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
        }
        //checks if transaction id exists and that it does not already have a feedback
        Optional<Transfer> transaction = transferRepository.findTransferById(request.transactionId());
        if(transaction.isEmpty()) {
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        } else if (!transaction.get().getFeedback().isEmpty()) {
            return new ResponseEntity<>(HttpStatus.CONFLICT);
        }

        //retries card for max values
        ActiveCard card = activeCardRepository.findActiveCardsByNumber(transaction.get().getCardNumber()).get();

        // checks if transaction status and feedback are the same
        if(transaction.get().getResult().equals(request.feedback)) {
            return new ResponseEntity<>(HttpStatus.UNPROCESSABLE_ENTITY);
        }
        //transaction status was allowed
        else if (transaction.get().getResult().equals(statuses.get(0))) {
            //new status is manual so decreasing limit
            if(request.feedback.equals(statuses.get(1))){
                card.setMaxAllowed((int) Math.ceil(0.8 * card.getMaxAllowed() - 0.2 * transaction.get().getAmount()));
            }
            //new status is prohibited so decreasing both limits
            else {
                card.setMaxAllowed((int) Math.ceil(0.8 * card.getMaxAllowed() - 0.2 * transaction.get().getAmount()));
                card.setMaxManual((int) Math.ceil(0.8 * card.getMaxManual() - 0.2 * transaction.get().getAmount()));
            }
        }
        //transaction status was manual
        else if (transaction.get().getResult().equals(statuses.get(1))) {
            //new status is allowed so increasing limit
            if(request.feedback.equals(statuses.get(0))){
                card.setMaxAllowed((int) Math.ceil(0.8 * card.getMaxAllowed() + 0.2 * transaction.get().getAmount()));
            }
            //new status is prohibited so decreasing limit
            else {
                card.setMaxManual((int) Math.ceil(0.8 * card.getMaxManual() - 0.2 * transaction.get().getAmount()));
            }
        }
        //transaction status was prohibited
        else if (transaction.get().getResult().equals(statuses.get(2))) {
            //new status is allowed so increasing both limits
            if(request.feedback.equals(statuses.get(0))){
                card.setMaxAllowed((int) Math.ceil(0.8 * card.getMaxAllowed() + 0.2 * transaction.get().getAmount()));
                card.setMaxManual((int) Math.ceil(0.8 * card.getMaxManual() + 0.2 * transaction.get().getAmount()));
            }
            //new status is manual so increasing limit
            else {
                card.setMaxManual((int) Math.ceil(0.8 * card.getMaxManual() + 0.2 * transaction.get().getAmount()));
            }
        }
        //uppdate transaction
        transaction.get().setFeedback(request.feedback);
        transferRepository.save(transaction.get());
        //update card with new limits
        activeCardRepository.save(card);

        ObjectMapper objectMapper = new ObjectMapper();
       return new ResponseEntity<>(objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(transaction.get()),HttpStatus.OK);
    }

    @PostMapping(value = "/api/antifraud/transaction", produces = MediaType.TEXT_HTML_VALUE)
    public ResponseEntity<String> newTransaction (@Valid @RequestBody transaction request) {

        //check ip and cardnumber
        String regex = "^(\\d{16})$";
        if (!request.number.matches(regex) || !LuhnCheckDigit.LUHN_CHECK_DIGIT.isValid(request.number)) {
            return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
        }
        InetAddressValidator validator = InetAddressValidator.getInstance();
        if (!validator.isValid(request.ip)){
            return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
        }


        //see if card exists and if not create it.
        Optional<ActiveCard> card = activeCardRepository.findActiveCardsByNumber(request.number);
        if(card.isEmpty()) {
            ActiveCard newCard = new ActiveCard();
            newCard.setNumber(request.number);
            activeCardRepository.save(newCard);
        }
        ActiveCard activeCard = activeCardRepository.findActiveCardsByNumber(request.number).get();

        try {
            //checks transaction status based on amount and max limits
            result = test(request.amount, activeCard.getMaxAllowed(), activeCard.getMaxManual());
            //amount is not a positive number
            if (result.equals("BAD REQUEST")) {
                return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
            }

            String json = """
                        {
                          "result": "%s",
                          "info": "%s"
                        }
                    """;
            //sees if ip or card is blacklisted
            boolean stolenCard = cardRepository.findCardByNumber(request.number).isPresent();
            boolean suspiciousIp = ipRepository.findIpByIp(request.ip).isPresent();

            Transfer transfer = new Transfer();
            transfer.setAmount(request.amount);
            transfer.setIpAddress(request.ip);
            transfer.setCardNumber(request.number);
            transfer.setRegion(request.region);
            transfer.setTransactionDate(request.date);

            //finds all transfers for the last hour for a card and puts region and ip i setList for further use
            Iterable<Transfer> transfers = transferRepository.findTransferByCardNumberAndTransactionDateBetween(request.number, LocalDateTime.parse(request.date).minusHours(1).toString(), request.date);
            Set <String> region = new HashSet<>();
            Set <String> ip = new HashSet<>();
            for (Transfer t : transfers){
                region.add(t.getRegion());
                ip.add(t.getIpAddress());
            }
            //remove current region and ip fo only others remain
            region.remove(request.region);
            ip.remove(request.ip);

            //used for info in json string
            StringBuilder info = new StringBuilder();


            //saves the original status based on amount
            String tempRes = result;

            if(stolenCard || suspiciousIp) {
                result = "PROHIBITED";
            }
            //if there are 2 other regions or ips the request becomes MANUAL_PROCESSING
            if((region.size() == 2 || ip.size() == 2) && !result.equals("PROHIBITED")) {
                result = "MANUAL_PROCESSING";
            }
            //if 3 or more it becomes PROHIBITED
            else if (region.size() >= 3 || ip.size() >= 3) {
                result = "PROHIBITED";
            }
            //an exception for if amount is manual but the end result is PROHIBITED it should then not add amount
            if(!(tempRes.equals("MANUAL_PROCESSING") && result.equals("PROHIBITED"))) {
                if (request.amount < activeCard.getMaxAllowed()) {

                }
                else {
                    info.append("amount");
                }
            }

            
            //saves transfer and returns a json
            if (Objects.equals(result,"ALLOWED")) {
                transfer.setResult("ALLOWED");
                transferRepository.save(transfer);
                return new ResponseEntity<String>(json.formatted(result, "none"), HttpStatus.OK);
            } else  {
                //builds info string
                if (stolenCard) {
                    if(!info.isEmpty()) {
                        info.append(", card-number");
                    } else {
                        info.append("card-number");
                    }
                }
                if (suspiciousIp) {
                    if(!info.isEmpty()) {
                       info.append(", ip");
                    } else {
                       info.append("ip");
                    }
                }
                if(ip.size() >= 2) {
                    if(!info.isEmpty()) {
                        info.append(", ip-correlation");
                    } else {
                        info.append("ip-correlation");
                    }    
                }
                if(region.size() >= 2) {
                    if(!info.isEmpty()) {
                        info.append(", region-correlation");
                    } else {
                        info.append("region-correlation");
                    }
                }

                transfer.setResult(result);
                transferRepository.save(transfer);
                return new ResponseEntity<>(json.formatted(result, info.toString()), HttpStatus.OK);
            }
        } catch (Exception ignore) {
            return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
        }
    }
    //list of stolen cards
    @GetMapping(path = "api/antifraud/stolencard")
    public ResponseEntity<String> getCards() throws JsonProcessingException {
        return new ResponseEntity<>(listOfObject(cardRepository.findAllByOrderByIdAsc()),HttpStatus.OK);
    }

    //add stolen card to
    @PostMapping(path = "api/antifraud/stolencard")
    public ResponseEntity<String> insertCards(@RequestBody putCardRequest request) throws JsonProcessingException {
        //check if valid number
        String regex = "^(\\d{16})$";
        if (!request.number.matches(regex) || !LuhnCheckDigit.LUHN_CHECK_DIGIT.isValid(request.number)) {
            return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
        }
        //see if card is already registered
        Optional<StolenCard> user = cardRepository.findCardByNumber(request.number);
        if (user.isPresent()) {
            return new ResponseEntity<>(HttpStatus.CONFLICT);
        }
        //create and save card
        StolenCard card = new StolenCard();
        card.setNumber(request.number);
        cardRepository.save(card);
        Optional<StolenCard> newCard = cardRepository.findCardByNumber(request.number);
        ObjectMapper objectMapper = new ObjectMapper();
        return new ResponseEntity<>(objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(newCard.get()),HttpStatus.OK);
    }
    //remove card from list
    @Transactional
    @DeleteMapping(path = "api/antifraud/stolencard/{number}")
    public ResponseEntity<String> deleteCards(@PathVariable String number){
        String regex = "^(\\d{16})$";
        if (!number.matches(regex) || !LuhnCheckDigit.LUHN_CHECK_DIGIT.isValid(number)) {
            return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
        }
        Optional<StolenCard> cardForRemoval = cardRepository.findCardByNumber(number);
        if (cardForRemoval.isEmpty()) {
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        }

        cardRepository.deleteCardByNumber(number);
        String output = """
                {
                   "status": "Card %s successfully removed!"
                }
                """;
        return new ResponseEntity<>(output.formatted(number),HttpStatus.OK);}

    //get list of saved ip addresses
    @GetMapping(path = "api/antifraud/suspicious-ip")
    public ResponseEntity<String> getIp() throws JsonProcessingException {
        return new ResponseEntity<>(listOfObject(ipRepository.findAllByOrderByIdAsc()),HttpStatus.OK);
    }
    //add suspicious ip address
    @PostMapping(path = "api/antifraud/suspicious-ip")
    public ResponseEntity<String> insertIp(@RequestBody putIpRequest request) throws JsonProcessingException {
        //check if address is valid
        InetAddressValidator validator = InetAddressValidator.getInstance();
        if (!validator.isValid(request.ip)){
            return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
        }
        //check if ip is already registered
        Optional<SuspiciousIp> user = ipRepository.findIpByIp(request.ip);
        if (user.isPresent()) {
            return new ResponseEntity<>(HttpStatus.CONFLICT);
        }
        //create and save ip
        SuspiciousIp ip = new SuspiciousIp();
        ip.setIp(request.ip());
        ipRepository.save(ip);
        Optional<SuspiciousIp> newIp = ipRepository.findIpByIp(request.ip);
        ObjectMapper objectMapper = new ObjectMapper();
        return new ResponseEntity<>(objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(newIp.get()),HttpStatus.OK);
    }
    //remove ip from list
    @Transactional
    @DeleteMapping(path = "api/antifraud/suspicious-ip/{ip}")
    public ResponseEntity<String> deleteIp(@PathVariable String ip){
        System.out.println("removing ip " + ip);
        InetAddressValidator validator = InetAddressValidator.getInstance();
        if (!validator.isValid(ip)){
            return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
        }
        Optional<SuspiciousIp> ipForRemoval = ipRepository.findIpByIp(ip);
        if (ipForRemoval.isEmpty()) {
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        }
        ipRepository.deleteIpByIp(ip);
        String output = """
                {
                   "status": "IP %s successfully removed!"
                }
                """;
        return new ResponseEntity<>(output.formatted(ip),HttpStatus.OK);
    }

    //takes a list and returns a json list as string
    public static String listOfObject (List<?> objects) throws JsonProcessingException {
        ObjectMapper objectMapper = new ObjectMapper();
        StringBuilder output = new StringBuilder();
        output.append("[\n");
        for (Object c : objects) {
            output.append("\t").append(objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(c)).append(",");
        }
        output.deleteCharAt(output.length() - 1);
        output.append("\n]");
        return output.toString();
    }

    // different request bodies
    record putCardRequest(String number) {}
    record putIpRequest(String ip) {}
    record transaction(int amount, @NotBlank String ip, @NotBlank String number, @NotBlank String region, @NotBlank String date) {}
    record feedback(long transactionId, String feedback) {};
}
