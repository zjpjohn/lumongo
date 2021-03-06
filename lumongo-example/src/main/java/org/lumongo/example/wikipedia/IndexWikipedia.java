package org.lumongo.example.wikipedia;

import info.bliki.wiki.filter.PlainTextConverter;
import info.bliki.wiki.model.WikiModel;
import org.apache.log4j.Logger;
import org.lumongo.client.command.Store;
import org.lumongo.client.config.LumongoPoolConfig;
import org.lumongo.client.pool.LumongoWorkPool;
import org.lumongo.client.result.CreateOrUpdateIndexResult;
import org.lumongo.client.result.StoreResult;
import org.lumongo.example.wikipedia.schema.ContributorType;
import org.lumongo.example.wikipedia.schema.PageType;
import org.lumongo.example.wikipedia.schema.RedirectType;
import org.lumongo.example.wikipedia.schema.RevisionType;
import org.lumongo.example.wikipedia.schema.TextType;
import org.lumongo.fields.Mapper;
import org.lumongo.util.LogUtil;
import org.lumongo.xml.StaxJAXBReader;

import java.io.File;
import java.util.List;
import java.util.concurrent.Future;

public class IndexWikipedia {

	@SuppressWarnings("unused")
	private final static Logger log = Logger.getLogger(IndexWikipedia.class);

	private static Mapper<Article> mapper;

	private static LumongoWorkPool lumongoWorkPool;

	private static WikiModel wikiModel = new WikiModel("/wiki/${image}", "/wiki/${title}");
	private static PlainTextConverter plainTextConverter = new PlainTextConverter();

	private final static boolean stripMarkup = false;

	public static void main(String[] args) throws Exception {

		if (args.length != 2) {
			System.out.println("Usage: filename lumongoServers");
			System.out.println(" ex. /tmp/enwiki-20120802-pages-articles-multistream.xml 10.0.0.10,10.0.0.11");
			System.out.println(" download from http://dumps.wikimedia.org/enwiki/20120802/enwiki-20120802-pages-articles-multistream.xml.bz2");
			System.out.println(" a single active lumongo server is enough, cluster membership will be updated when a connection is established");
			System.exit(1);
		}

		String fileName = args[0];
		String[] servers = args[1].split(",");

		if (!(new File(fileName)).exists()) {
			System.out.println("File <" + fileName + "> does not exist");
			System.exit(2);
		}

		LogUtil.loadLogConfig();

		LumongoPoolConfig lumongoPoolConfig = new LumongoPoolConfig();
		lumongoPoolConfig.setDefaultRetries(servers.length - 1); //?
		for (String server : servers) {
			lumongoPoolConfig.addMember(server);
		}
		lumongoWorkPool = new LumongoWorkPool(lumongoPoolConfig);
		lumongoWorkPool.updateMembers();

		mapper = new Mapper<Article>(Article.class);

		@SuppressWarnings("unused")
		CreateOrUpdateIndexResult createOrUpdateIndexResult = lumongoWorkPool.createOrUpdateIndex(mapper.createOrUpdateIndex());

		StaxJAXBReader<PageType> s = new StaxJAXBReader<PageType>(PageType.class, "page", 1) {

			private int counter = 0;
			private long start = System.currentTimeMillis();
			private long last = System.currentTimeMillis();

			@Override
			public void handle(PageType item) throws Exception {
				final Article article = formArticle(item);
				if (article != null) {

					Store store = mapper.createStore(article);

					@SuppressWarnings("unused")
					Future<StoreResult> sr = lumongoWorkPool.storeAsync(store);

				}
				if (++counter % 5000 == 0) {
					long end = System.currentTimeMillis();
					long timeForSet = end - last;
					long timeSinceStart = end - start;
					System.out.println(counter + "\t" + timeForSet + "\t" + timeSinceStart);
					last = end;
				}
			}

		};

		s.handleFile(fileName);

		lumongoWorkPool.shutdown();
	}

	public static Article formArticle(final PageType page) {
		RedirectType redirectType = page.getRedirect();
		if (redirectType != null) {
			// skip redirects
			return null;
		}

		Article article = new Article();
		article.setTitle(page.getTitle());
		article.setNamespace(page.getNs().intValue());
		article.setId(page.getId().toString());

		List<Object> revUploadList = page.getRevisionOrUpload();

		for (Object o : revUploadList) {
			if (o instanceof RevisionType) {
				RevisionType revisionType = (RevisionType) o;

				TextType textType = revisionType.getText();
				String wikiText = textType.getValue();

				if (stripMarkup) {
					String plainText = wikiModel.render(plainTextConverter, wikiText);
					article.setText(plainText);
				}
				else {
					article.setText(wikiText);
				}
				article.setRevision(revisionType.getId().longValue());

				ContributorType contributorType = revisionType.getContributor();
				if (contributorType != null) {
					if (contributorType.getId() != null) {
						article.setUserId(contributorType.getId().intValue());
					}
					article.setUser(contributorType.getUsername());
				}
				article.setRevisionDate(revisionType.getTimestamp().toGregorianCalendar().getTime());

				break;
			}
		}
		return article;
	}

}
