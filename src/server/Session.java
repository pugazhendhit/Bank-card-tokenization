package server;

import primitives.UserDatabase;
import exceptions.CardReadingDenied;
import primitives.CreditCard;
import primitives.User;
import exceptions.IncorrectUserPassword;
import exceptions.TokenRegistrationDenied;
import exceptions.IncorrectUserName;
import exceptions.LoginException;
import exceptions.TokenNotRegistered;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Serializable;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.TreeMap;
import primitives.Token;

public class Session implements Serializable {

    private User user;
    transient private UserDatabase db = new UserDatabase();

    /**
     *
     * @param userToLog
     * @throws IncorrectUserPassword
     * @throws IncorrectUserName
     * @throws LoginException
     */
    public Session(User userToLog)
            throws IncorrectUserPassword, IncorrectUserName, LoginException {
        if (userToLog == null) {
            throw new LoginException();
        } else {
            logIn(userToLog);
        }
    }

    /**
     *
     * @param userToLog
     * @throws IncorrectUserPassword
     * @throws IncorrectUserName
     */
    public void logIn(User userToLog)
            throws IncorrectUserPassword, IncorrectUserName {

        User registredUser = db.getUserByName(userToLog.getUsername());

        //is the entered password correct
        if (registredUser.getPassword()
                .equals(userToLog.getPassword())) {

            this.user = registredUser;

            //set the privileges of the user.
            this.user.setCanReadCardId(true);
            this.user.setCanRegisterToken(true);

        } else {
            throw new IncorrectUserPassword();
        }
    }

    /**
     *
     * @param cardId
     * @return
     * @throws TokenRegistrationDenied
     */
    public Token registerCard(CreditCard cardId)
            throws TokenRegistrationDenied {

        Token token;
        System.out.println("Registering the card");
        if (user.canRegisterToken()) {

            do {
                token = cardId.tokenize();
            } while (!isUnique(token));

            user.addTuple(token, cardId);
            new UserDatabase().updateDatabase(user);
            System.out.println("Registration done");
            return token;
        } else {
            throw new TokenRegistrationDenied();
        }
    }

    /**
     *
     * @param comparator
     * @param txtFile
     * @throws IOException
     * @throws CardReadingDenied
     * @throws TokenNotRegistered
     */
    public void exportSorted(Comparator comparator, File txtFile)
            throws IOException, CardReadingDenied, TokenNotRegistered {

        if (!user.canReadCardId()) {
            throw new CardReadingDenied();
        }
        TreeMap<Token, CreditCard> sortedMap = new TreeMap<>(comparator);
        sortedMap.putAll(user.getTokenMap());

        String header = String.format("%-20s%-20s\n", "Token: ", "CardId: ");
        String tableRow;
        BufferedWriter bw = new BufferedWriter(new FileWriter(txtFile));

        bw.write(header);

        for (Entry<Token, CreditCard> entry : sortedMap.entrySet()) {
            tableRow = String.format("%-20s%-20s\n", entry.getKey(), entry.getValue());
            System.out.print(tableRow);
            bw.write(tableRow);
        }

        bw.close();

    }

    /**
     *
     * @param token
     * @return
     * @throws CardReadingDenied
     * @throws TokenNotRegistered
     */
    public CreditCard getCardId(Token token)
            throws CardReadingDenied, TokenNotRegistered {
        if (user.canReadCardId()) {

            Map<Token, CreditCard> tokenMap = user.getTokenMap();

            if (tokenMap == null) {
                throw new TokenNotRegistered();
            }

            CreditCard card = tokenMap.get(token);

            if (card == null) {
                throw new TokenNotRegistered();
            } else {
                return card;
            }

        } else {
            throw new CardReadingDenied();
        }
    }

    /**
     *
     * @return
     */
    public User getUser() {
        return user;
    }

    /**
     *
     */
    public void close() {
        user.setCanReadCardId(false);
        user.setCanRegisterToken(false);
        new UserDatabase().updateDatabase(user);

    }

    private boolean isUnique(Token token) {
        List<User> list = db.deserialize();

        for (User user : list) {
            if (user.getTokenMap().containsKey(token)) {
                return false;
            }
        }
        return true;
    }

    @Override
    public String toString() {
        return "Session{" + "user=" + user + '}';
    }

}
