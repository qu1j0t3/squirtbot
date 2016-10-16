package squirt

import org.apache.commons.collections.map.LRUMap

class TweetCache {
  val chanCache  = new LRUMap(100)  // tested for repetitions in a particular channel
  val tweetCache = new LRUMap(300)  // recent tweets so we can look up deletions, etc

  def lookupOrPut(chan: String, t: Tweet): Boolean =
    chanCache.synchronized {
      val key = chan+"/"+t.id
      if(chanCache.containsKey(key)) {
        true
      } else {
        chanCache.put(key, ())
        tweetCache.synchronized { tweetCache.put(t.id, t) }
        false
      }
    }

  def getTweetById(id: String): Option[Tweet] =
    tweetCache.synchronized {
      tweetCache.get(id) match {
        case t: Tweet => Some(t)
        case _        => None
      }
    }
}
