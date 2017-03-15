package netCrawl;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.jsoup.Connection;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

class crawlUtils {
	public static Document getDocument(String href) {
		Connection con = Jsoup.connect(href);
		Document document = null;
		try {
			document = con.get();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return document;
	}
}

class PlayLists {
	static private List<PlayList> playLists = new ArrayList<>();
	static private List<CrawlPlayListsThread> threads = new ArrayList<>();
	private static int count = 0;
	static private boolean finish = true;

	static public boolean isFinish() {
		finish = true;
		for (CrawlPlayListsThread thread : threads) {
			finish = finish && !thread.isAlive();
		}
		return finish;
	}

	static public List<PlayList> getPlayLists() {
		return playLists;
	}

	static public void crawlPlayLists() {
		Document document = crawlUtils.getDocument("http://music.163.com/discover/playlist");
		Elements elements = document.select("div#m-pl-pager div.u-page a");
		count = Integer.parseInt(elements.get(elements.size() - 2).html());
		int threadCount = 4;
		for (int i = 0, offset = count / threadCount; i < threadCount; i++) {
			if (i == threadCount - 1)
				threads.add(new CrawlPlayListsThread(i * offset, count));
			else
				threads.add(new CrawlPlayListsThread(i * offset, (i + 1) * offset));
		}
		for (Thread thread : threads) {
			thread.start();
		}
	}

}

class CrawlPlayListsThread extends Thread {
	private int begin, end;

	public CrawlPlayListsThread(int begin, int end) {
		this.begin = begin;
		this.end = end;
	}

	@Override
	public void run() {
		for (int i = begin; i < end; i++) {
			String playListsHref = "http://music.163.com/discover/playlist/?order=hot&cat=全部&limit=35&offset=" + i * 35;
			Document document = crawlUtils.getDocument(playListsHref);
			Elements elements = document.select("ul#m-pl-container li");
			for (Element element : elements) {
				String playListHref = "http://music.163.com" + element.select("p.dec a").get(0).attr("href");
				PlayLists.getPlayLists().add(new PlayList(playListHref));
			}

		}
	}

}

class PlayList {
	private String href;
	private static List<Song> songs = new ArrayList<>();
	static private List<CrawlSongsThread> threads = new ArrayList<>();
	static private boolean finish = true;

	static public boolean isFinish() {
		finish = true;
		for (CrawlSongsThread thread : threads) {
			finish = finish && !thread.isAlive();
		}
		return finish;
	}

	public PlayList(String href) {
		this.href = href;
	}

	public String getHref() {
		return href;
	}

	static public List<Song> getSongs() {
		return songs;
	}

	public static void crawlSongs() {
		int count = PlayLists.getPlayLists().size();

		int threadCount = 5;
		for (int i = 0, offset = count / threadCount; i < threadCount; i++) {
			if (i == threadCount - 1)
				threads.add(new CrawlSongsThread(i * offset, count));
			else
				threads.add(new CrawlSongsThread(i * offset, (i + 1) * offset));
		}
		for (Thread thread : threads) {
			thread.start();
		}
	}
}

class CrawlSongsThread extends Thread {
	private int begin, end;

	public CrawlSongsThread(int begin, int end) {
		this.begin = begin;
		this.end = end;
	}

	@Override
	public void run() {
		for (int i = begin; i < end; i++) {

			PlayList playList = PlayLists.getPlayLists().get(i);
			Document document = crawlUtils.getDocument(playList.getHref());
			Elements elements = document.select("div#song-list-pre-cache ul.f-hide li");
			for (Element element : elements) {
				Elements hrefElement = element.select("a");
				String href = "http://music.163.com" + hrefElement.get(0).attr("href");
				String name = hrefElement.get(0).html();
				Song song = new Song(href, name);
				PlayList.getSongs().add(song);
			}
		}
	}

}

class Song {
	private String href;
	private String name;
	private String singer;
	private int numberOfComments;
	private boolean uncrawl = true;
	static private List<CrawlThread> threads = new ArrayList<>();
	static private boolean finish = true;

	static public boolean isFinish() {
		finish = true;
		for (CrawlThread thread : threads) {
			finish = finish && !thread.isAlive();
		}
		return finish;
	}

	public Song(String href, String name) {
		super();
		this.href = href;
		this.name = name;
	}

	public String getHref() {
		return href;
	}

	public void setHref(String href) {
		this.href = href;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getSinger() {
		return singer;
	}

	public void setSinger(String singer) {
		this.singer = singer;
	}

	public void setNumberOfComments(int numberOfComments) {
		this.numberOfComments = numberOfComments;
	}

	public boolean isUncrawl() {
		return uncrawl;
	}

	public void setUncrawl(boolean uncrawl) {
		this.uncrawl = uncrawl;
	}

	public static void crawl() {
		int count = PlayList.getSongs().size();

		int threadCount = 5;
		for (int i = 0, offset = count / threadCount; i < threadCount; i++) {
			if (i == threadCount - 1)
				threads.add(new CrawlThread(i * offset, count));
			else
				threads.add(new CrawlThread(i * offset, (i + 1) * offset));
		}
		for (Thread thread : threads) {
			thread.start();
		}
	}

	public int getNumberOfComments() {
		return numberOfComments;
	}
}

class CrawlThread extends Thread {
	private int begin, end;

	public CrawlThread(int begin, int end) {
		this.begin = begin;
		this.end = end;
	}

	@Override
	public void run() {
		for (int i = begin; i < end; i++) {
			Song song = PlayList.getSongs().get(i);
			if (!song.isUncrawl())
				continue;
			song.setUncrawl(false);
			Document document = crawlUtils.getDocument(song.getHref());
			String str = document.select("title").get(0).html();
			try {
				song.setSinger(str.substring(str.indexOf("-") + 1, str.lastIndexOf("-")));
			} catch (Exception e1) {
				// TODO Auto-generated catch block
				System.out.println("str"+song.getHref());
			}
			String id = song.getHref().substring(song.getHref().indexOf("id=") + 3);
			try {
				song.setNumberOfComments(EncryptTools.commentCount(id));
			} catch (Exception e) {
				System.out.println("comm:"+song.getHref());
			}
			if (song.getNumberOfComments() > 10000) {
				System.out.println("歌曲名字：" + song.getName() + "    歌手：" + song.getSinger() + "    评论："
						+ song.getNumberOfComments());
			}
		}
	}

}

public class Music163Crawl {
	public static void main(String[] args) throws Exception {
		System.out.println("第一阶段：");
		PlayLists.crawlPlayLists();
		while (!PlayLists.isFinish())
			Thread.sleep(1000);
		System.out.println("第二阶段：");
		PlayList.crawlSongs();
		while (!PlayList.isFinish())
			Thread.sleep(1000);
		System.out.println("第三阶段：");
		System.out.println(PlayList.getSongs().size());
		Song.crawl();

	}
}
