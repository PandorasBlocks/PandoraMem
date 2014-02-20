package me.DawnBudgie.PandoraMem;

import java.util.logging.Logger;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

public class PandoraMem
  extends JavaPlugin
  implements CommandExecutor
{
  private static final long MEGABYTE = 1048576L;
  public static FileConfiguration config;
  private static Logger log;
  public static PandoraMem plugin;
  
  public static void infoConsole(String msg)
  {
    log.info("[MemoryChecker] " + msg);
  }
  
  public static void warningConsole(String msg)
  {
    log.warning("[MemoryChecker] " + msg);
  }
  
  public static long bytesToMegabytes(long bytes)
  {
    return bytes / 1048576L;
  }
  
  public void onEnable()
  {
    plugin = this;
    log = Logger.getLogger("Minecraft");
    log.info("@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@");
    loadConfig();
    getCommand("memory").setExecutor(this);
    getCommand("mem").setExecutor(this);
    getConfig().createSection("Memory.var");
    getConfig().createSection("Memory.var2");
    if (getConfig().getBoolean("Memory.auto-restart"))
    {
      infoConsole(getConfig().getString("Messages.info.automatic-restart-on"));
      initialRestart();
    }
    else
    {
      infoConsole(getConfig().getString("Messages.info.automatic-restart-off"));
    }
    if (getConfig().getLong("Memory.time.freeup") != 0L)
    {
      infoConsole(getConfig().getString("Messages.info.automatic-on"));
      if (getConfig().getBoolean("Memory.debug")) {
        initialDelayDebug();
      } else {
        initialDelay();
      }
    }
    else
    {
      infoConsole(getConfig().getString("Messages.info.automatic-off"));
    }
    log.info("@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@");
  }
  
  public void onDisable()
  {
    getConfig().set("Memory.var2", null);
    getConfig().set("Memory.var", null);
  }
  
  public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args)
  {
    String MC = ChatColor.BLUE + "[MemoryChecker]=======================================";
    
    String noperm = getConfig().getString("Messages.fail.no-perm");
    if ((sender instanceof Player))
    {
      Player player = (Player)sender;
      if ((args.length < 1) && (player.hasPermission("memory.free")))
      {
        FreeS(player);
        return false;
      }
      if ((args[0].equalsIgnoreCase("check")) && (player.hasPermission("memory.check")))
      {
        Memory(player);
        return false;
      }
      if ((args[0].equalsIgnoreCase("reload")) && (player.hasPermission("memory.reload")))
      {
        reloadConfig();
        player.sendMessage(ChatColor.BLUE + "MemoryChecker reloaded!");
        return false;
      }
      if ((args[0].equalsIgnoreCase("restart")) && (player.hasPermission("memory.restart")))
      {
        Restart();
        return false;
      }
      player.sendMessage(MC);
      player.sendMessage(ChatColor.BLUE + "|| " + ChatColor.RED + noperm);
      
      return false;
    }
    if (args.length < 1)
    {
      FreeSConsole();
      return false;
    }
    if (args[0].equalsIgnoreCase("check"))
    {
      MemoryConsole();
      return false;
    }
    if (args[0].equalsIgnoreCase("reload"))
    {
      reloadConfig();
      infoConsole("MemoryChecker reloaded!");
      return false;
    }
    if (args[0].equalsIgnoreCase("restart"))
    {
      Restart();
      return false;
    }
    infoConsole("Bad args!");
    return false;
  }
  
  public void initialDelayDebug()
  {
    Long time = Long.valueOf(getConfig().getLong("Memory.time.freeup"));
    final String freed = config.getString("Messages.info.freed-memory");
    
    final Runtime runtime = Runtime.getRuntime();
    getServer().getScheduler().scheduleSyncRepeatingTask(this, new Runnable()
    {
      public void run()
      {
        PandoraMem.this.getConfig().set("Memory.var2", Long.toString(runtime.freeMemory()));
        
        PandoraMem.this.saveConfig();
        
        runtime.gc();
        Long memory = Long.valueOf(Long.parseLong(PandoraMem.this.getConfig().getString("Memory.var2")) - runtime.freeMemory());
        if (memory.longValue() <= 1048576L)
        {
        	PandoraMem.infoConsole("Nothing freed up;/");
        }
        else
        {
        	PandoraMem.infoConsole(freed + PandoraMem.bytesToMegabytes(memory.longValue()) + " Mb!");
          for (Player player : Bukkit.getOnlinePlayers()) {
            if (player.isOp()) {
              player.sendMessage(ChatColor.BLUE + freed + PandoraMem.bytesToMegabytes(memory.longValue()) + " Mb!");
            }
          }
        }
      }
    }, 20L, time.longValue() * 20L);
  }
  
  public void initialDelay()
  {
    Long time = Long.valueOf(getConfig().getLong("Memory.time.freeup"));
    final Runtime runtime = Runtime.getRuntime();
    getServer().getScheduler().scheduleSyncRepeatingTask(this, new Runnable()
    {
      public void run()
      {
        runtime.gc();
      }
    }, 20L, time.longValue() * 20L);
  }
  
  public void initialRestart()
  {
    Long time2 = Long.valueOf(getConfig().getLong("Memory.time.restart"));
    final int minram = config.getInt("Memory.min-ram");
    final Runtime runtime = Runtime.getRuntime();
    getServer().getScheduler().scheduleSyncRepeatingTask(this, new Runnable()
    {
      public void run()
      {
    	  PandoraMem.this.saveConfig();
        if (PandoraMem.bytesToMegabytes(runtime.freeMemory()) <= minram)
        {
        	PandoraMem.infoConsole("Free RAM: " + PandoraMem.bytesToMegabytes(runtime.freeMemory()));
          PandoraMem.infoConsole(PandoraMem.config.getString("Messages.restart.console"));
          for (Player player : Bukkit.getOnlinePlayers()) {
            player.kickPlayer(ChatColor.RED + PandoraMem.config.getString("Messages.restart.players"));
          }
          Bukkit.savePlayers();
          Bukkit.shutdown();
        }
      }
    }, 20L, time2.longValue() * 20L);
  }
  
  public void Memory(Player player)
  {
    String MC = ChatColor.BLUE + "[MemoryChecker]=======================================";
    
    String used = config.getString("Messages.info.used-memory");
    String total = config.getString("Messages.info.total-memory");
    String free = config.getString("Messages.info.free-memory");
    Runtime runtime = Runtime.getRuntime();
    long memoryF = runtime.freeMemory();
    long memoryT = runtime.totalMemory();
    long memory = memoryT - memoryF;
    player.sendMessage(MC);
    player.sendMessage(ChatColor.BLUE + "|| " + ChatColor.WHITE + used + ChatColor.GOLD + bytesToMegabytes(memory) + " Mb");
    
    player.sendMessage(ChatColor.BLUE + "|| " + ChatColor.WHITE + free + ChatColor.GOLD + bytesToMegabytes(memoryF) + " Mb");
    
    player.sendMessage(ChatColor.BLUE + "|| " + ChatColor.WHITE + total + ChatColor.GOLD + bytesToMegabytes(memoryT) + " Mb");
  }
  
  public void MemoryConsole()
  {
    String used = config.getString("Messages.info.used-memory");
    String total = config.getString("Messages.info.total-memory");
    String free = config.getString("Messages.info.free-memory");
    Runtime runtime = Runtime.getRuntime();
    long memoryF = runtime.freeMemory();
    long memoryT = runtime.totalMemory();
    long memory = memoryT - memoryF;
    infoConsole("|| " + used + bytesToMegabytes(memory) + " Mb");
    infoConsole("|| " + free + bytesToMegabytes(memoryF) + " Mb");
    infoConsole("|| " + total + bytesToMegabytes(memoryT) + " Mb");
  }
  
  public void FreeS(final Player player)
  {
    String MC = ChatColor.BLUE + "[MemoryChecker]=======================================";
    
    final String freed = config.getString("Messages.info.freed-memory");
    String ft = config.getString("Messages.fail.first-try");
    final String st = config.getString("Messages.fail.second-try");
    String t = config.getString("Messages.fail.trying");
    final Runtime runtime = Runtime.getRuntime();
    getConfig().set("Memory.var", Long.toString(runtime.freeMemory()));
    saveConfig();
    runtime.gc();
    Long memory = Long.valueOf(Long.parseLong(getConfig().getString("Memory.var")) - runtime.freeMemory());
    if (memory.longValue() <= 1048576L)
    {
      player.sendMessage(MC);
      player.sendMessage(ChatColor.BLUE + "|| " + ChatColor.RED + ft);
      player.sendMessage(ChatColor.BLUE + "|| " + ChatColor.YELLOW + t);
      
      getServer().getScheduler().scheduleSyncDelayedTask(this, new Runnable()
      {
        public void run()
        {
          Long memory = Long.valueOf(Long.parseLong(PandoraMem.this.getConfig().getString("Memory.var")) - runtime.freeMemory());
          if (memory.longValue() > 1048576L) {
            player.sendMessage(ChatColor.BLUE + "|| " + ChatColor.GREEN + freed + ChatColor.GOLD + PandoraMem.bytesToMegabytes(memory.longValue()) + " Mb!");
          } else {
            player.sendMessage(ChatColor.BLUE + "|| " + ChatColor.RED + st);
          }
        }
      }, 40L);
    }
    else
    {
      player.sendMessage(MC);
      player.sendMessage(ChatColor.BLUE + "|| " + ChatColor.GREEN + freed + ChatColor.GOLD + bytesToMegabytes(memory.longValue()) + " Mb!");
      
      player.sendMessage(ChatColor.BLUE + "|| " + ChatColor.YELLOW + t);
      getServer().getScheduler().scheduleSyncDelayedTask(this, new Runnable()
      {
        public void run()
        {
          Long memory = Long.valueOf(Long.parseLong(PandoraMem.this.getConfig().getString("Memory.var")) - runtime.freeMemory());
          if (memory.longValue() > 1048576L) {
            player.sendMessage(ChatColor.BLUE + "|| " + ChatColor.GREEN + freed + ChatColor.GOLD + PandoraMem.bytesToMegabytes(memory.longValue()) + " Mb!");
          } else {
            player.sendMessage(ChatColor.BLUE + "|| " + ChatColor.RED + st);
          }
        }
      }, 40L);
    }
  }
  
  public void FreeSConsole()
  {
    final String freed = config.getString("Messages.info.freed-memory");
    String ft = config.getString("Messages.fail.first-try");
    final String st = config.getString("Messages.fail.second-try");
    String t = config.getString("Messages.fail.trying");
    final Runtime runtime = Runtime.getRuntime();
    getConfig().set("Memory.var", Long.toString(runtime.freeMemory()));
    saveConfig();
    runtime.gc();
    Long memory = Long.valueOf(Long.parseLong(getConfig().getString("Memory.var")) - runtime.freeMemory());
    if (memory.longValue() <= 1048576L)
    {
      infoConsole("|| " + ft);
      infoConsole("|| " + t);
      
      getServer().getScheduler().scheduleSyncDelayedTask(this, new Runnable()
      {
        public void run()
        {
          Long memory = Long.valueOf(Long.parseLong(PandoraMem.this.getConfig().getString("Memory.var")) - runtime.freeMemory());
          if (memory.longValue() > 1048576L) {
        	  PandoraMem.infoConsole("|| " + freed + PandoraMem.bytesToMegabytes(memory.longValue()) + " Mb!");
          } else {
        	  PandoraMem.infoConsole("|| " + st);
          }
        }
      }, 40L);
    }
    else
    {
      infoConsole(ChatColor.BLUE + "|| " + ChatColor.GREEN + freed + ChatColor.GOLD + bytesToMegabytes(memory.longValue()) + " Mb!");
      
      infoConsole(ChatColor.BLUE + "|| " + ChatColor.YELLOW + t);
      getServer().getScheduler().scheduleSyncDelayedTask(this, new Runnable()
      {
        public void run()
        {
          Long memory = Long.valueOf(Long.parseLong(PandoraMem.this.getConfig().getString("Memory.var")) - runtime.freeMemory());
          if (memory.longValue() > 1048576L) {
        	  PandoraMem.infoConsole("|| " + freed + PandoraMem.bytesToMegabytes(memory.longValue()) + " Mb!");
          } else {
        	  PandoraMem.infoConsole("|| " + st);
          }
        }
      }, 40L);
    }
  }
  
  public void Restart()
  {
    infoConsole(config.getString("Messages.restart.console"));
    for (Player player : Bukkit.getOnlinePlayers()) {
      player.kickPlayer(ChatColor.RED + config.getString("Messages.restart.players"));
    }
    Bukkit.savePlayers();
    Bukkit.shutdown();
  }
  
  public void loadConfig()
  {
    config = getConfig();
    config.options().copyDefaults(true);
    saveConfig();
    infoConsole("Config loaded!");
  }
}
