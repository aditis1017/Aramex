package customsmanagement

import java.util.Properties
import java.util.regex.Pattern

import javax.mail.BodyPart
import javax.mail.Folder
import javax.mail.Message
import javax.mail.Multipart
import javax.mail.Part
import javax.mail.Session
import javax.mail.Store
import javax.mail.search.ComparisonTerm
import javax.mail.search.ReceivedDateTerm

import com.kms.katalon.core.annotation.Keyword
import com.kms.katalon.core.util.KeywordUtil

class MicrosoftEmailOtpFetcher {

    private static String extractTextFromPart(Part p) throws Exception {
        if (p.isMimeType('text/*')) {
            Object c = p.getContent()
            return c != null ? c.toString() : ''
        }
        if (p.isMimeType('multipart/*')) {
            Multipart mp = (Multipart) p.getContent()
            StringBuilder sb = new StringBuilder()
            for (int i = 0; i < mp.getCount(); i++) {
                BodyPart bp = mp.getBodyPart(i)
                sb.append(extractTextFromPart(bp)).append('\n')
            }
            return sb.toString()
        }
        return ''
    }

    private static String messageText(Message m) {
        try {
            Object c = m.getContent()
            if (c instanceof String) {
                return (String) c
            }
            if (c instanceof Multipart) {
                return extractTextFromPart(m)
            }
        } catch (Exception e) {
            KeywordUtil.logInfo("Could not read message body: ${e.message}")
        }
        return ''
    }

    static String extractOtpFromText(String text, String customRegex) {
        if (!text) {
            return ''
        }
        List<String> patterns = []
        if (customRegex?.trim()) {
            patterns << customRegex.trim()
        }
        patterns << '(?i)(?:code|verification code|one[- ]?time code|security info|use the following code)\\s*[:is]\\s*(\\d{6,8})'
        patterns << '(?i)\\b(\\d{6})\\b'
        for (String p : patterns) {
            def m1 = (text =~ Pattern.compile(p))
            if (m1.find()) {
                return m1.group(1)
            }
        }
        return ''
    }

    static String fetchOtpFromImap(String host, String port, String user, String password, String senderContains,
            int lookbackMinutes, String customRegex, int maxWaitSeconds, int pollIntervalSeconds) {
        if (!host || !user || !password) {
            throw new IllegalStateException('IMAP host, user, and password are required for OTP auto-fetch.')
        }
        int portNum = Integer.parseInt((port ?: '993').trim())
        String senderNeedle = (senderContains ?: 'microsoft').toLowerCase()
        long deadline = System.currentTimeMillis() + (maxWaitSeconds * 1000L)
        Date since = new Date(System.currentTimeMillis() - (lookbackMinutes * 60L * 1000L))

        Properties props = new Properties()
        props.setProperty('mail.store.protocol', 'imaps')
        props.setProperty('mail.imaps.host', host)
        props.setProperty('mail.imaps.port', String.valueOf(portNum))
        props.setProperty('mail.imaps.ssl.enable', 'true')
        props.setProperty('mail.imaps.connectiontimeout', '20000')
        props.setProperty('mail.imaps.timeout', '20000')

        Session session = Session.getInstance(props)
        Store store = null
        Folder inbox = null
        try {
            while (System.currentTimeMillis() < deadline) {
                store = session.getStore('imaps')
                store.connect(host, portNum, user, password)
                inbox = store.getFolder('INBOX')
                inbox.open(Folder.READ_ONLY)

                ReceivedDateTerm newer = new ReceivedDateTerm(ComparisonTerm.GE, since)
                Message[] msgs = inbox.search(newer)
                List<Message> sorted = (msgs as List).sort { a, b -> safeDate(b) <=> safeDate(a) }

                for (Message mm : sorted) {
                    String from = ''
                    try {
                        def fa = mm.getFrom()
                        if (fa) {
                            from = fa.collect { it.toString() }.join(' ')
                        }
                    } catch (Exception ignored) {
                    }
                    if (!from.toLowerCase().contains(senderNeedle)) {
                        continue
                    }
                    String subj = (mm.getSubject() ?: '')
                    String body = messageText(mm)
                    String blob = (subj + '\n' + body)
                    String otp = extractOtpFromText(blob, customRegex)
                    if (otp) {
                        KeywordUtil.logInfo('OTP extracted from mailbox (value not logged).')
                        try { inbox.close(false) } catch (Exception ignored) {}
                        try { store.close() } catch (Exception ignored) {}
                        inbox = null
                        store = null
                        return otp
                    }
                }

                inbox.close(false)
                store.close()
                inbox = null
                store = null

                Thread.sleep(Math.max(2, pollIntervalSeconds) * 1000L)
            }
        } finally {
            try {
                if (inbox != null && inbox.isOpen()) {
                    inbox.close(false)
                }
            } catch (Exception ignored) {
            }
            try {
                if (store != null && store.isConnected()) {
                    store.close()
                }
            } catch (Exception ignored) {
            }
        }
        return ''
    }

    private static Date safeDate(Message m) {
        try {
            Date d = m.getReceivedDate()
            if (d != null) return d
        } catch (Exception ignored) {
        }
        try {
            return m.getSentDate()
        } catch (Exception ignored) {
            return new Date(0)
        }
    }

    @Keyword
    String fetchLatestMicrosoftOtpFromImapUsingProfile() {
        String host = ProfileGlobals.get('MS_IMAP_HOST')
        String port = ProfileGlobals.get('MS_IMAP_PORT', '993')
        String user = ProfileGlobals.get('MS_IMAP_USER')
        String pass = ProfileGlobals.get('MS_IMAP_PASSWORD')
        String sender = ProfileGlobals.get('MS_OTP_SENDER_CONTAINS', 'microsoft')
        int lookback = Integer.parseInt(ProfileGlobals.get('MS_OTP_LOOKBACK_MINUTES', '10'))
        String regex = ProfileGlobals.get('MS_OTP_REGEX')
        int maxWait = Integer.parseInt(ProfileGlobals.get('MS_OTP_FETCH_MAX_WAIT_SECONDS', '90'))
        int poll = Integer.parseInt(ProfileGlobals.get('MS_OTP_FETCH_POLL_SECONDS', '5'))
        return fetchOtpFromImap(host, port, user, pass, sender, lookback, regex, maxWait, poll)
    }
}