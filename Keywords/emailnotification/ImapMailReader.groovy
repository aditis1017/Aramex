package emailnotification

import java.util.Properties

import javax.mail.BodyPart
import javax.mail.Folder
import javax.mail.Message
import javax.mail.Multipart
import javax.mail.Part
import javax.mail.Session
import javax.mail.Store
import javax.mail.search.ComparisonTerm
import javax.mail.search.ReceivedDateTerm

import com.kms.katalon.core.util.KeywordUtil

/**
 * Low-level IMAP reader for notification email verification.
 * Reuses the same jakarta.mail dependency as {@link customsmanagement.MicrosoftEmailOtpFetcher}.
 */
class ImapMailReader {

	static Map readMessageContent(Message message) {
		String from = ''
		try {
			def fa = message.getFrom()
			if (fa) {
				from = fa.collect { it.toString() }.join(' ')
			}
		} catch (Exception ignored) {
		}
		String subject = (message.getSubject() ?: '').trim()
		String body = messageText(message)
		Date received = safeReceivedDate(message)
		return [
			from    : from,
			subject : subject,
			body    : body,
			received: received != null ? received.toString() : ''
		]
	}

	static List fetchMessagesSince(Map imapConfig, Date since) {
		String host = (imapConfig.host ?: '').trim()
		String user = (imapConfig.user ?: '').trim()
		String password = (imapConfig.password ?: '').trim()
		int port = Integer.parseInt((imapConfig.port ?: '993').toString().trim())
		if (!host || !user || !password) {
			throw new IllegalStateException('IMAP host, user, and password are required for email notification verification.')
		}

		Properties props = new Properties()
		props.setProperty('mail.store.protocol', 'imaps')
		props.setProperty('mail.imaps.host', host)
		props.setProperty('mail.imaps.port', String.valueOf(port))
		props.setProperty('mail.imaps.ssl.enable', 'true')
		props.setProperty('mail.imaps.connectiontimeout', '20000')
		props.setProperty('mail.imaps.timeout', '20000')

		Session session = Session.getInstance(props)
		Store store = null
		Folder inbox = null
		List results = []
		try {
			store = session.getStore('imaps')
			store.connect(host, port, user, password)
			inbox = store.getFolder('INBOX')
			inbox.open(Folder.READ_ONLY)

			ReceivedDateTerm newer = new ReceivedDateTerm(ComparisonTerm.GE, since)
			Message[] msgs = inbox.search(newer)
			List sorted = (msgs as List).sort { a, b -> safeReceivedDate(b) <=> safeReceivedDate(a) }
			for (Message mm : sorted) {
				results << readMessageContent(mm)
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
		return results
	}

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
			KeywordUtil.logInfo("Could not read notification email body: ${e.message}")
		}
		return ''
	}

	private static Date safeReceivedDate(Message m) {
		try {
			Date d = m.getReceivedDate()
			if (d != null) {
				return d
			}
		} catch (Exception ignored) {
		}
		try {
			return m.getSentDate()
		} catch (Exception ignored) {
			return new Date(0)
		}
	}

	static String normalizeEmailText(String text) {
		if (!text) {
			return ''
		}
		return text
			.replaceAll(/<[^>]+>/, ' ')
			.replaceAll(/&nbsp;/, ' ')
			.replaceAll(/\s+/, ' ')
			.trim()
	}
}
