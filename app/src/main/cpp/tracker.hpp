#pragma once
#include <vector>
#include <array>
#include <cstdint>
#include <cmath>
#include <algorithm>
#include <limits>
namespace ase {
struct Point {float x=0,y=0,q=0; bool valid=false;};
struct Feature {int dx,dy; std::vector<float> ref;};
class Tracker {
 int w=0,h=0,miss=0; float x=0,y=0; std::vector<Feature> features; std::vector<float> scores;
 static constexpr int R=5;
 bool inside(int a,int b) const {return a>=R&&b>=R&&a<w-R&&b<h-R;}
 std::vector<float> patch(const uint8_t* im,int a,int b) const {
   if(!inside(a,b))return {}; std::vector<float> p; p.reserve(121); float sum=0,ss=0;
   for(int j=-R;j<=R;j++)for(int i=-R;i<=R;i++){float v=im[(b+j)*w+a+i];p.push_back(v);sum+=v;ss+=v*v;}
   float m=sum/p.size(),n=std::sqrt(std::max(0.f,ss-sum*m)); if(n<100)return {}; for(auto& v:p)v=(v-m)/n;return p;
 }
 float score(const uint8_t* im,int a,int b,const std::vector<float>& p) const {
   if(!inside(a,b))return -1; float sum=0,ss=0,dot=0;int k=0;
   for(int j=-R;j<=R;j++)for(int i=-R;i<=R;i++){float v=im[(b+j)*w+a+i];sum+=v;ss+=v*v;dot+=v*p[k++];}
   float n=ss-sum*sum/k;return n>=10000?dot/std::sqrt(n):-1;
 }
 public:
 bool seed(const uint8_t* im,int W,int H,int a,int b,int radius) {
   features.clear();w=W;h=H;miss=0;x=a;y=b;if(!im||W<24||H<24||!inside(a,b)||radius<8)return false;
   int d=std::clamp(radius/2,6,30); const int offsets[][2]={{0,0},{-d,0},{d,0},{0,-d},{0,d},{-d,-d},{d,d}};
   for(auto& o:offsets){auto p=patch(im,a+o[0],b+o[1]);if(!p.empty())features.push_back({o[0],o[1],std::move(p)});}
   return features.size()>=3;
 }
 private:
 Point search(const uint8_t* im,int range,bool rescue){
   int cx=std::lround(x),cy=std::lround(y);
   struct Candidate{float x,y,q;};std::array<Candidate,7> candidates{};size_t count=0;
   const int side=2*range+1;scores.resize(side*side);
   for(const auto& f:features){int bx=0,by=0;float best=-2; // exhaustive integer lattice; no skipped offsets
     for(int j=-range;j<=range;j++)for(int i=-range;i<=range;i++){float q=score(im,cx+f.dx+i,cy+f.dy+j,f.ref);scores[(j+range)*side+i+range]=q;if(q>best){best=q;bx=cx+i;by=cy+j;}}
     if(best<(rescue?.90f:.78f))continue;float second=-2;
     for(int j=-range;j<=range;j++)for(int i=-range;i<=range;i++){int a=cx+i,b=cy+j;if(std::abs(a-bx)<=3&&std::abs(b-by)<=3)continue;second=std::max(second,scores[(j+range)*side+i+range]);}
     if(best-second>=(rescue?.08f:.035f)){
       // Subpixel quadratic interpolation, only for a well-defined interior peak.
       auto delta=[](float l,float m,float r){float d=l-2*m+r;return d<-.015f?std::clamp(.5f*(l-r)/d,-.5f,.5f):0.f;};
       float fx=0,fy=0;int i=bx-cx+range,j=by-cy+range;
       if(best<.995f&&i>0&&j>0&&i<side-1&&j<side-1){int k=j*side+i;fx=delta(scores[k-1],best,scores[k+1]);fy=delta(scores[k-side],best,scores[k+side]);}
       candidates[count++]={bx+fx,by+fy,best};
     }
   }
   int bestN=0;float bestQ=0,sx=0,sy=0;
   for(size_t i=0;i<count;i++){const auto& a=candidates[i];int n=0;float q=0,xx=0,yy=0;for(size_t j=0;j<count;j++){const auto& b=candidates[j];float dx=a.x-b.x,dy=a.y-b.y;if(dx*dx+dy*dy<=4){n++;q+=b.q;xx+=b.x;yy+=b.y;}}if(n>bestN||(n==bestN&&q>bestQ)){bestN=n;bestQ=q;sx=xx;sy=yy;}}
   if(bestN<(rescue?5:3)||bestN*2<int(features.size())||!inside(std::lround(sx/std::max(1,bestN)),std::lround(sy/std::max(1,bestN)))){return {};}
   return {sx/bestN,sy/bestN,bestQ/bestN,true};
 }
 public:
 Point step(const uint8_t* im,int W,int H){
   if(!im||W!=w||H!=h||features.size()<3||miss>=8)return {};
   const int range=std::min(32,12+miss*5);
   auto q=search(im,range,false);
   // One bounded wide rescue per missing run; fixed seed templates, stricter consensus.
   if(!q.valid&&miss==0&&features.size()>=5)q=search(im,32,true);
   if(!q.valid){miss++;return {};}
   x=q.x;y=q.y;miss=0;return q;
 }
};
}
