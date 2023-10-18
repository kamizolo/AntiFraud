package antifraud.Rest;

import antifraud.Card.CardRepository;
import antifraud.Card.StolenCard;
import antifraud.Ip.IpRepository;
import antifraud.Ip.SuspiciousIp;
import antifraud.Transfer.Transfer;
import antifraud.Transfer.TransferRepository;
import antifraud.User.User;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.commons.validator.routines.InetAddressValidator;
import org.apache.tomcat.util.json.JSONParser;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.apache.commons.validator.routines.checkdigit.LuhnCheckDigit;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;


import javax.transaction.Transactional;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

@Controller
public class Transaction {

    @Autowired
    IpRepository ipRepository;
    @Autowired
    CardRepository cardRepository;

    @Autowired
    TransferRepository transferRepository;
    String result = "";
    public String test(long amount) {
        if (amount <= 0) {
            return "BAD REQUEST";
        } else if (amount <= 200) {
            return "ALLOWED";
        } else if (amount <= 1500) {
            return "MANUAL_PROCESSING";
        } else {
            return "PROHIBITED";
        }
    }
    @PostMapping(value = "/api/antifraud/transaction", produces = MediaType.TEXT_HTML_VALUE)
    public ResponseEntity<String> result(@RequestBody transaction request) {
        //System.out.println("body: " + body + "|End");
        try {

            String test = String.valueOf(request.amount);

            result = test(Long.parseLong(test));
            String json = """
                        {
                          "result": "%s",
                          "info": "%s"
                        }
                    """;
            boolean stolenCard = cardRepository.findCardByNumber(request.number).isPresent();

            boolean suspiciousIp = ipRepository.findIpByIp(request.ip).isPresent();

            Transfer transfer = new Transfer();
            transfer.setAmount(request.amount);
            transfer.setIpAddress(request.ip);
            transfer.setCardNumber(request.number);
            transfer.setRegion(request.region);
            transfer.setTransactionDate(LocalDateTime.parse(request.date));

            Iterable<Transfer> users = transferRepository.findTransactionByCardNumberAndTransactionDateBetween(request.number, LocalDateTime.parse(request.date).minusHours(1), LocalDateTime.parse(request.date));

            if (Objects.equals(result, "BAD REQUEST")) {
                return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
            }
            else if (Objects.equals(result, "ALLOWED")) {
                transferRepository.save(transfer);
                return new ResponseEntity<String>(json.formatted(result, "none"), HttpStatus.OK);
            } else if (Objects.equals(result, "MANUAL_PROCESSING") && (!stolenCard && !suspiciousIp)) {
                transferRepository.save(transfer);
                return new ResponseEntity<>(json.formatted(result, "amount"), HttpStatus.OK);
            } else  {

                StringBuilder info = new StringBuilder();
                if(result.equals("PROHIBITED")) {
                    info.append("amount");
                }
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
                if(info.charAt(info.length() -1) == ',') {
                    info.deleteCharAt(info.length() - 1);
                }
                transferRepository.save(transfer);
                return new ResponseEntity<>(json.formatted("PROHIBITED", info.toString()), HttpStatus.OK);
            }
        } catch (Exception ignore) {
            return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
        }
    }

    @GetMapping(path = "api/antifraud/stolencard")
    public ResponseEntity<String> getCards() throws JsonProcessingException {
        return new ResponseEntity<>(listOfObject(cardRepository.findAllByOrderByIdAsc()),HttpStatus.OK);
    }


    @PostMapping(path = "api/antifraud/stolencard")
    public ResponseEntity<String> insertCards(@RequestBody putCardRequest request) throws JsonProcessingException {
        String regex = "^(\\d{16})$";
        if (!request.number.matches(regex) || !LuhnCheckDigit.LUHN_CHECK_DIGIT.isValid(request.number)) {
            return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
        }
        Optional<StolenCard> user = cardRepository.findCardByNumber(request.number);
        if (user.isPresent()) {
            return new ResponseEntity<>(HttpStatus.CONFLICT);
        }
        StolenCard card = new StolenCard();
        card.setNumber(request.number);
        cardRepository.save(card);
        Optional<StolenCard> newCard = cardRepository.findCardByNumber(request.number);
        ObjectMapper objectMapper = new ObjectMapper();
        return new ResponseEntity<>(objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(newCard.get()),HttpStatus.OK);}
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
    @GetMapping(path = "api/antifraud/suspicious-ip")
    public ResponseEntity<String> getIp() throws JsonProcessingException {
        return new ResponseEntity<>(listOfObject(ipRepository.findAllByOrderByIdAsc()),HttpStatus.OK);
    }
    @PostMapping(path = "api/antifraud/suspicious-ip")
    public ResponseEntity<String> insertIp(@RequestBody putIpRequest request) throws JsonProcessingException {
        System.out.println("adding ip " + request.ip);
        InetAddressValidator validator = InetAddressValidator.getInstance();
        if (!validator.isValid(request.ip)){
            return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
        }
        Optional<SuspiciousIp> user = ipRepository.findIpByIp(request.ip);
        if (user.isPresent()) {
            return new ResponseEntity<>(HttpStatus.CONFLICT);
        }
        SuspiciousIp ip = new SuspiciousIp();
        ip.setIp(request.ip());
        ipRepository.save(ip);
        Optional<SuspiciousIp> newIp = ipRepository.findIpByIp(request.ip);
        System.out.println("*added ip " + newIp.get().getIp() + "+");
        ObjectMapper objectMapper = new ObjectMapper();
        return new ResponseEntity<>(objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(newIp.get()),HttpStatus.OK);}
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

    public static String listOfObject (List<?> objects) throws JsonProcessingException {
        ObjectMapper objectMapper = new ObjectMapper();
        StringBuilder output = new StringBuilder();
        output.append("[\n");
        for (Object c : objects) {
            output.append(objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(c)).append(",");
        }
        output.deleteCharAt(output.length() - 1);
        output.append("\n]");
        System.out.println(output.toString());
        return output.toString();
    }
    record putCardRequest(String number) {}
    record putIpRequest(String ip) {}

    record transaction(String amount, String ip, String number, String region, String date) {}
}
