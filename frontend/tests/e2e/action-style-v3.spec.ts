import {mkdtempSync,mkdirSync,writeFileSync} from 'node:fs'
import {tmpdir} from 'node:os'
import {join} from 'node:path'
import {test,expect,chromium} from '@playwright/test'
import {newOwner,beginFood,addFood,foodReview,confirmFood,baseURL} from './v3-journeys'
import {todayInZone} from '../../src/domain/dates'
test('T32 centered single-line feedback and compact toolbar across five widths',async({page,browser})=>{
 await newOwner(page,'v32-center');const observations=[]
 for(const width of [360,375,768,1280,1440]){
  await page.setViewportSize({width,height:900});await beginFood(page,todayInZone());await addFood(page);await foodReview(page);await confirmFood(page)
  const toast=page.locator('.save-feedback-toast'),box=await toast.boundingBox();expect(box).not.toBeNull();expect(Math.abs(box!.x+box!.width/2-width/2)).toBeLessThan(2);expect(Math.abs(box!.y+box!.height/2-450)).toBeLessThan(2)
  await expect(toast).toHaveText('Food saved');expect(await toast.evaluate(el=>{const s=getComputedStyle(el.querySelector('.MuiAlert-message')!);return {wrap:getComputedStyle(el.querySelector('.MuiAlert-root')!).whiteSpace,height:el.clientHeight,font:s.fontSize}})).toMatchObject({wrap:'nowrap'})
  expect(await page.evaluate(()=>document.documentElement.scrollWidth<=innerWidth)).toBe(true)
  await page.screenshot({path:`test-results/ux-v3/v32-center-${width}.png`})
  await expect(toast).toHaveCount(0,{timeout:3000});const refresh=page.getByRole('button',{name:'Refresh',exact:true}),today=page.getByRole('button',{name:'Today',exact:true})
  if(width>=1280){const r=await refresh.boundingBox(),t=await today.boundingBox();expect(Math.abs(r!.y-t!.y)).toBeLessThan(2)}
  await expect(today).toBeDisabled();await expect(page.getByRole('link',{name:'Add food',exact:true})).toBeVisible();await expect(page.getByRole('link',{name:'Add exercise',exact:true})).toBeVisible()
  await page.getByRole('button',{name:'Close saved notice',exact:true}).click();await expect(page.getByRole('link',{name:'View records',exact:true})).toHaveCount(0)
  observations.push({width,box})
 }
 await test.info().attach('center-observations',{body:JSON.stringify({version:browser.version(),observations}),contentType:'application/json'});await page.goto(baseURL+'/dashboard')
})

test('T32 actual browser 200 percent zoom keeps saved feedback and actions usable',async()=>{
 const profile=mkdtempSync(join(tmpdir(),'fitnutri-v32-zoom-'));mkdirSync(join(profile,'Default'));writeFileSync(join(profile,'Default','Preferences'),JSON.stringify({partition:{default_zoom_level:{x:Math.log(2)/Math.log(1.2)}}}))
 const context=await chromium.launchPersistentContext(profile,{headless:true,viewport:null,deviceScaleFactor:undefined,isMobile:undefined,hasTouch:undefined,args:['--window-size=1280,900'],...(process.env.FITNUTRI_E2E_BROWSER_PATH?{executablePath:process.env.FITNUTRI_E2E_BROWSER_PATH}:{})})
 try{const page=context.pages()[0];await newOwner(page,'v32-zoom');const dimensions=await page.evaluate(()=>({width:innerWidth,height:innerHeight,outer:outerWidth,dpr:devicePixelRatio,cssZoom:getComputedStyle(document.documentElement).zoom}));expect(Math.abs(dimensions.outer/dimensions.width-2)).toBeLessThan(.1);expect(dimensions.cssZoom).toBe('1')
 await beginFood(page,todayInZone());await addFood(page);await foodReview(page);await confirmFood(page);const notice=page.locator('.save-feedback-toast'),box=await notice.boundingBox();expect(Math.abs(box!.x+box!.width/2-dimensions.width/2)).toBeLessThan(2);expect(Math.abs(box!.y+box!.height/2-dimensions.height/2)).toBeLessThan(2);expect(await page.evaluate(()=>document.documentElement.scrollWidth<=innerWidth)).toBe(true);await page.screenshot({path:'test-results/ux-v3/v32-actual-zoom200.png'});await expect(notice).toHaveCount(0,{timeout:3000});await page.getByRole('link',{name:'View records',exact:true}).click();await page.getByRole('link',{name:'View',exact:true}).click();await expect(page.getByRole('dialog',{name:'Saved entry details',exact:true})).toBeVisible();await page.getByRole('button',{name:'Close',exact:true}).click();await test.info().attach('actual-browser-zoom',{body:JSON.stringify(dimensions),contentType:'application/json'})
 }finally{await context.close()}
})
